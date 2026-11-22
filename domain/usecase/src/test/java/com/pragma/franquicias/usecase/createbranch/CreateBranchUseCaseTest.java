package com.pragma.franquicias.usecase.createbranch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pragma.franquicias.model.branch.Branch;
import com.pragma.franquicias.model.branch.exceptions.DuplicateBranchNameException;
import com.pragma.franquicias.model.branch.exceptions.InvalidBranchNameException;
import com.pragma.franquicias.model.branch.gateways.SaveBranchGateway;
import com.pragma.franquicias.model.franchise.exceptions.FranchiseNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CreateBranchUseCaseTest {

    private static final String TOO_LONG_NAME = "a".repeat(121);
    private static final UUID FRANCHISE_ID = UUID.randomUUID();

    @Mock
    private SaveBranchGateway saveBranchGateway;

    private CreateBranchUseCase useCase() {
        return new CreateBranchUseCase(saveBranchGateway);
    }

    @Test
    void createsBranchWithTrimmedNameGeneratedIdAndTheGivenFranchise() {
        when(saveBranchGateway.save(any(Branch.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Branch.class)));

        StepVerifier.create(useCase().execute(FRANCHISE_ID, "  Centro  "))
                .assertNext(branch -> {
                    assertNotNull(branch.getId());
                    assertEquals(FRANCHISE_ID, branch.getFranchiseId());
                    assertEquals("Centro", branch.getName());
                })
                .verifyComplete();

        verify(saveBranchGateway, times(1)).save(any(Branch.class));
    }

    static String[] invalidNames() {
        return new String[] {null, "", "   ", TOO_LONG_NAME, "  " + TOO_LONG_NAME + "  "};
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    void rejectsNullBlankOrTooLongNameWithoutCallingTheGateway(String invalidName) {
        StepVerifier.create(useCase().execute(FRANCHISE_ID, invalidName))
                .expectError(InvalidBranchNameException.class)
                .verify();

        verifyNoInteractions(saveBranchGateway);
    }

    @Test
    void rejectsAMissingFranchiseIdWithoutCallingTheGateway() {
        StepVerifier.create(useCase().execute(null, "Centro"))
                .expectError(NullPointerException.class)
                .verify();

        verifyNoInteractions(saveBranchGateway);
    }

    @Test
    void acceptsTheMaximumLengthAfterTrim() {
        String maxName = "a".repeat(120);
        when(saveBranchGateway.save(any(Branch.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Branch.class)));

        StepVerifier.create(useCase().execute(FRANCHISE_ID, " " + maxName + " "))
                .assertNext(branch -> assertEquals(maxName, branch.getName()))
                .verifyComplete();
    }

    @Test
    void propagatesDuplicateNameError() {
        when(saveBranchGateway.save(any(Branch.class)))
                .thenReturn(Mono.error(new DuplicateBranchNameException("Centro")));

        StepVerifier.create(useCase().execute(FRANCHISE_ID, "Centro"))
                .expectError(DuplicateBranchNameException.class)
                .verify();
    }

    @Test
    void propagatesFranchiseNotFoundError() {
        when(saveBranchGateway.save(any(Branch.class)))
                .thenReturn(Mono.error(new FranchiseNotFoundException(FRANCHISE_ID)));

        StepVerifier.create(useCase().execute(FRANCHISE_ID, "Centro"))
                .expectError(FranchiseNotFoundException.class)
                .verify();
    }
}
