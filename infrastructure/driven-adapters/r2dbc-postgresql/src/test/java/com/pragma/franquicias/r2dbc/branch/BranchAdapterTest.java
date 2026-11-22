package com.pragma.franquicias.r2dbc.branch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.pragma.franquicias.model.branch.Branch;
import com.pragma.franquicias.model.branch.exceptions.DuplicateBranchNameException;
import com.pragma.franquicias.model.franchise.exceptions.FranchiseNotFoundException;
import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BranchAdapterTest {

    private static final String FOREIGN_KEY_VIOLATION = "23503";
    private static final String CHECK_VIOLATION = "23514";

    @Mock
    private R2dbcEntityTemplate template;

    @Spy
    private ResilientOperations resilient = new ResilientOperations(new ResilienceProperties(
            Duration.ofSeconds(2), 3, Duration.ofMillis(5), 50, 100, 100, Duration.ofSeconds(30), 3));

    @InjectMocks
    private BranchAdapter adapter;

    private final Branch branch =
            Branch.builder().id(UUID.randomUUID()).franchiseId(UUID.randomUUID()).name("Centro").build();

    private static DataIntegrityViolationException integrityViolation(String sqlState) {
        return new DataIntegrityViolationException(
                "violation", new R2dbcDataIntegrityViolationException("violation", sqlState));
    }

    @Test
    void insertsTheBranchAndReturnsTheStoredOne() {
        when(template.insert(any(BranchData.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, BranchData.class)));

        StepVerifier.create(adapter.save(branch))
                .assertNext(saved -> assertEquals(branch, saved))
                .verifyComplete();
    }

    @Test
    void translatesAUniqueViolationIntoADuplicateNameError() {
        when(template.insert(any(BranchData.class))).thenReturn(Mono.error(new DuplicateKeyException("duplicate")));

        StepVerifier.create(adapter.save(branch))
                .expectError(DuplicateBranchNameException.class)
                .verify();
    }

    @Test
    void translatesAForeignKeyViolationIntoAFranchiseNotFoundError() {
        when(template.insert(any(BranchData.class)))
                .thenReturn(Mono.error(integrityViolation(FOREIGN_KEY_VIOLATION)));

        StepVerifier.create(adapter.save(branch))
                .expectError(FranchiseNotFoundException.class)
                .verify();
    }

    @Test
    void doesNotDisguiseOtherIntegrityViolationsAsAMissingFranchise() {
        when(template.insert(any(BranchData.class))).thenReturn(Mono.error(integrityViolation(CHECK_VIOLATION)));

        StepVerifier.create(adapter.save(branch))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    @Test
    void anInfrastructureFailureBecomesServiceUnavailableAfterTheRetries() {
        when(template.insert(any(BranchData.class)))
                .thenReturn(Mono.error(new DataAccessResourceFailureException("connection lost")));

        StepVerifier.create(adapter.save(branch))
                .expectError(ServiceUnavailableException.class)
                .verify();
    }

    @Test
    void letsOtherDatabaseErrorsPassUntouched() {
        when(template.insert(any(BranchData.class)))
                .thenReturn(Mono.error(new IllegalStateException("boom")));

        StepVerifier.create(adapter.save(branch))
                .expectError(IllegalStateException.class)
                .verify();
    }
}
