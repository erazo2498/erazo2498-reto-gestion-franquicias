package com.pragma.franquicias.r2dbc.resilience;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Every value has a default, so the application needs no configuration to be resilient.
@ConfigurationProperties(prefix = "adapters.resilience")
public record ResilienceProperties(
        @DefaultValue("3s") Duration timeout,
        @DefaultValue("3") int maxAttempts,
        @DefaultValue("100ms") Duration backoff,
        @DefaultValue("50") int failureRateThreshold,
        @DefaultValue("10") int slidingWindowSize,
        @DefaultValue("5") int minimumNumberOfCalls,
        @DefaultValue("10s") Duration waitInOpenState,
        @DefaultValue("3") int permittedCallsInHalfOpen) {
}
