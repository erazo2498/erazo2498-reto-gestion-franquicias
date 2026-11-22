package com.pragma.franquicias.r2dbc.resilience;

import com.pragma.franquicias.model.common.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// One place that protects every call to the database with the three patterns, from the inside out:
//   1. Timeout: no attempt waits longer than `timeout`.
//   2. Retry with exponential backoff: only for transient connection failures, never for business errors (a duplicate
//      name will be a duplicate name again) and never after a timeout (the write may have happened).
//   3. Circuit breaker: after repeated infrastructure failures it opens and fails fast, so a database that is down
//      is not hammered. Business errors do not count as failures.
// What the client finally sees is a ServiceUnavailableException (503) instead of a raw infrastructure error.
public class ResilientOperations {

    private final Duration timeout;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public ResilientOperations(ResilienceProperties properties) {
        this.timeout = properties.timeout();
        this.retry = Retry.of(
                "database-retry",
                RetryConfig.custom()
                        .maxAttempts(properties.maxAttempts())
                        .intervalFunction(IntervalFunction.ofExponentialBackoff(properties.backoff(), 2.0))
                        .retryOnException(ResilientOperations::isTransient)
                        .build());
        this.circuitBreaker = CircuitBreaker.of(
                "database-circuit",
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(properties.failureRateThreshold())
                        .slidingWindowSize(properties.slidingWindowSize())
                        .minimumNumberOfCalls(properties.minimumNumberOfCalls())
                        .waitDurationInOpenState(properties.waitInOpenState())
                        .recordException(error -> isTransient(error) || error instanceof TimeoutException)
                        .build());
    }

    public <T> Mono<T> protect(Mono<T> operation) {
        return operation
                .timeout(timeout)
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorMap(ResilientOperations::isUnavailable, error -> new ServiceUnavailableException());
    }

    public <T> Flux<T> protectMany(Flux<T> operation) {
        return operation
                .timeout(timeout)
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorMap(ResilientOperations::isUnavailable, error -> new ServiceUnavailableException());
    }

    // A failure to reach or use the database, not an answer of the database about the data.
    private static boolean isTransient(Throwable error) {
        return error instanceof DataAccessResourceFailureException || error instanceof TransientDataAccessException;
    }

    private static boolean isUnavailable(Throwable error) {
        return isTransient(error) || error instanceof TimeoutException || error instanceof CallNotPermittedException;
    }
}
