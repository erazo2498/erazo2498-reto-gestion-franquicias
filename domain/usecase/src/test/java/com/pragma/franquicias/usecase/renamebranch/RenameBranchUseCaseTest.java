package com.pragma.franquicias.usecase.renamebranch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pragma.franquicias.model.branch.Branch;
import com.pragma.franquicias.model.branch.exceptions.BranchNotFoundException;
import com.pragma.franquicias.model.branch.exceptions.DuplicateBranchNameException;
import com.pragma.franquicias.model.branch.exceptions.InvalidBranchNameException;
import com.pragma.franquicias.model.branch.gateways.RenameBranchGateway;
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
class RenameBranchUseCaseTest {

    private static final UUID FRANCHISE_ID = UUID.randomUUID();
    private static final UUID BRANCH_ID = UUID.randomUUID();

    @Mock
    private RenameBranchGateway renameBranchGateway;

    private RenameBranchUseCase useCase() {
        return new RenameBranchUseCase(renameBranchGateway);
    }

    static String[] invalidNames() {
        return new String[] {null, "", "   ", "a".repeat(121)};
    }

    @Test
    void renamesWithTheTrimmedNameAndReturnsTheBranch() {
        when(renameBranchGateway.rename(FRANCHISE_ID, BRANCH_ID, "Sur"))
                .thenReturn(Mono.just(
                        Branch.builder().id(BRANCH_ID).franchiseId(FRANCHISE_ID).name("Sur").build()));

        StepVerifier.create(useCase().execute(FRANCHISE_ID, BRANCH_ID, "  Sur  "))
                .assertNext(branch -> assertEquals("Sur", branch.getName()))
                .verifyComplete();

        verify(renameBranchGateway).rename(FRANCHISE_ID, BRANCH_ID, "Sur");
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    void rejectsAnInvalidNameWithoutCallingTheGateway(String invalidName) {
        StepVerifier.create(useCase().execute(FRANCHISE_ID, BRANCH_ID, invalidName))
                .expectError(InvalidBranchNameException.class)
                .verify();

        verifyNoInteractions(renameBranchGateway);
    }

    @Test
    void failsWithBranchNotFoundWhenTheGatewayFindsNoBranch() {
        when(renameBranchGateway.rename(FRANCHISE_ID, BRANCH_ID, "Sur")).thenReturn(Mono.empty());

        StepVerifier.create(useCase().execute(FRANCHISE_ID, BRANCH_ID, "Sur"))
                .expectError(BranchNotFoundException.class)
                .verify();
    }

    @Test
    void propagatesTheDuplicateNameError() {
        when(renameBranchGateway.rename(FRANCHISE_ID, BRANCH_ID, "Sur"))
                .thenReturn(Mono.error(new DuplicateBranchNameException("Sur")));

        StepVerifier.create(useCase().execute(FRANCHISE_ID, BRANCH_ID, "Sur"))
                .expectError(DuplicateBranchNameException.class)
                .verify();
    }
}
