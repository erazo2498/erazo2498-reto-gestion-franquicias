package com.pragma.franquicias.r2dbc.resilience;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pragma.franquicias.model.common.ServiceUnavailableException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ResilientOperationsTest {

    // timeout, maxAttempts, backoff, failureRateThreshold, slidingWindowSize, minimumNumberOfCalls, waitInOpenState
    private static ResilientOperations operations(Duration timeout, int maxAttempts, int window, int minimumCalls) {
        return new ResilientOperations(new ResilienceProperties(
                timeout, maxAttempts, Duration.ofMillis(5), 50, window, minimumCalls, Duration.ofSeconds(30)));
    }

    private static ResilientOperations defaults() {
        return operations(Duration.ofSeconds(2), 3, 100, 100);
    }

    @Test
    void returnsTheValueOfAnOperationThatWorks() {
        StepVerifier.create(defaults().protect(Mono.just("ok"))).expectNext("ok").verifyComplete();
    }

    @Test
    void retriesATransientFailureAndThenSucceeds() {
        AtomicInteger attempts = new AtomicInteger();
        Mono<String> flaky = Mono.defer(() -> attempts.incrementAndGet() < 3
                ? Mono.error(new DataAccessResourceFailureException("connection lost"))
                : Mono.just("ok"));

        StepVerifier.create(defaults().protect(flaky)).expectNext("ok").verifyComplete();

        assertEquals(3, attempts.get());
    }

    @Test
    void givesUpAfterTheMaximumAttemptsAndAnswersServiceUnavailable() {
        AtomicInteger attempts = new AtomicInteger();
        Mono<String> alwaysDown = Mono.defer(() -> {
            attempts.incrementAndGet();
            return Mono.error(new DataAccessResourceFailureException("connection lost"));
        });

        StepVerifier.create(defaults().protect(alwaysDown))
                .expectError(ServiceUnavailableException.class)
                .verify();

        assertEquals(3, attempts.get());
    }

    @Test
    void doesNotRetryABusinessError() {
        AtomicInteger attempts = new AtomicInteger();
        Mono<String> duplicate = Mono.defer(() -> {
            attempts.incrementAndGet();
            return Mono.error(new DuplicateKeyException("duplicate"));
        });

        StepVerifier.create(defaults().protect(duplicate))
                .expectError(DuplicateKeyException.class)
                .verify();

        assertEquals(1, attempts.get());
    }

    @Test
    void cutsAnOperationThatTakesTooLongAndAnswersServiceUnavailable() {
        StepVerifier.create(operations(Duration.ofMillis(100), 3, 100, 100).protect(Mono.never()))
                .expectError(ServiceUnavailableException.class)
                .verify();
    }

    @Test
    void aTimeoutIsNotRetriedBecauseTheWriteMayHaveHappened() {
        AtomicInteger attempts = new AtomicInteger();
        Mono<String> slow = Mono.defer(() -> {
            attempts.incrementAndGet();
            return Mono.never();
        });

        StepVerifier.create(operations(Duration.ofMillis(100), 3, 100, 100).protect(slow))
                .expectError(ServiceUnavailableException.class)
                .verify();

        assertEquals(1, attempts.get());
    }

    @Test
    void theCircuitOpensAfterRepeatedFailuresAndFailsFastWithoutCallingTheOperation() {
        ResilientOperations operations = operations(Duration.ofSeconds(2), 1, 2, 2);
        AtomicInteger calls = new AtomicInteger();
        Mono<String> down = Mono.defer(() -> {
            calls.incrementAndGet();
            return Mono.error(new DataAccessResourceFailureException("connection lost"));
        });

        StepVerifier.create(operations.protect(down)).expectError(ServiceUnavailableException.class).verify();
        StepVerifier.create(operations.protect(down)).expectError(ServiceUnavailableException.class).verify();
        int callsBeforeTheCircuitOpened = calls.get();
        StepVerifier.create(operations.protect(down)).expectError(ServiceUnavailableException.class).verify();

        assertEquals(2, callsBeforeTheCircuitOpened);
        assertEquals(callsBeforeTheCircuitOpened, calls.get());
    }

    @Test
    void businessErrorsDoNotOpenTheCircuit() {
        ResilientOperations operations = operations(Duration.ofSeconds(2), 1, 2, 2);
        Mono<String> duplicate = Mono.error(new DuplicateKeyException("duplicate"));

        StepVerifier.create(operations.protect(duplicate)).expectError(DuplicateKeyException.class).verify();
        StepVerifier.create(operations.protect(duplicate)).expectError(DuplicateKeyException.class).verify();
        StepVerifier.create(operations.protect(duplicate)).expectError(DuplicateKeyException.class).verify();
        StepVerifier.create(operations.protect(Mono.just("still works"))).expectNext("still works").verifyComplete();
    }

    @Test
    void protectsAFluxToo() {
        StepVerifier.create(defaults().protectMany(Flux.just(1, 2, 3))).expectNext(1, 2, 3).verifyComplete();
    }

    @Test
    void aFluxThatKeepsFailingAnswersServiceUnavailable() {
        StepVerifier.create(defaults().protectMany(Flux.error(new DataAccessResourceFailureException("down"))))
                .expectError(ServiceUnavailableException.class)
                .verify();
    }
}
