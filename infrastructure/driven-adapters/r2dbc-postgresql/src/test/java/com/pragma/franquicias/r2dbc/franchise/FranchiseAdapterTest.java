package com.pragma.franquicias.r2dbc.franchise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.pragma.franquicias.model.franchise.Franchise;
import com.pragma.franquicias.model.franchise.exceptions.DuplicateFranchiseNameException;
import java.time.Duration;
import java.util.UUID;
import com.pragma.franquicias.model.common.ServiceUnavailableException;
import com.pragma.franquicias.r2dbc.resilience.ResilienceProperties;
import com.pragma.franquicias.r2dbc.resilience.ResilientOperations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class FranchiseAdapterTest {

    @Mock
    private R2dbcEntityTemplate template;

    @Spy
    private ResilientOperations resilient = new ResilientOperations(new ResilienceProperties(
            Duration.ofSeconds(2), 3, Duration.ofMillis(5), 50, 100, 100, Duration.ofSeconds(30)));

    @InjectMocks
    private FranchiseAdapter adapter;

    private final Franchise franchise = Franchise.builder().id(UUID.randomUUID()).name("Burger Co").build();

    @Test
    void insertsTheFranchiseAndReturnsTheStoredOne() {
        when(template.insert(any(FranchiseData.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, FranchiseData.class)));

        StepVerifier.create(adapter.save(franchise))
                .assertNext(saved -> assertEquals(franchise, saved))
                .verifyComplete();
    }

    @Test
    void translatesAUniqueViolationIntoADuplicateNameError() {
        when(template.insert(any(FranchiseData.class))).thenReturn(Mono.error(new DuplicateKeyException("duplicate")));

        StepVerifier.create(adapter.save(franchise))
                .expectError(DuplicateFranchiseNameException.class)
                .verify();
    }

    @Test
    void anInfrastructureFailureBecomesServiceUnavailableAfterTheRetries() {
        when(template.insert(any(FranchiseData.class)))
                .thenReturn(Mono.error(new DataAccessResourceFailureException("connection lost")));

        StepVerifier.create(adapter.save(franchise))
                .expectError(ServiceUnavailableException.class)
                .verify();
    }

    @Test
    void letsOtherDatabaseErrorsPassUntouched() {
        when(template.insert(any(FranchiseData.class)))
                .thenReturn(Mono.error(new IllegalStateException("boom")));

        StepVerifier.create(adapter.save(franchise))
                .expectError(IllegalStateException.class)
                .verify();
    }
}
