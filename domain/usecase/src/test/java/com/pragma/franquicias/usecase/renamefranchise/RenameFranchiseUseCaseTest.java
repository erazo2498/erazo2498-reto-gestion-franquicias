package com.pragma.franquicias.usecase.renamefranchise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pragma.franquicias.model.franchise.Franchise;
import com.pragma.franquicias.model.franchise.exceptions.DuplicateFranchiseNameException;
import com.pragma.franquicias.model.franchise.exceptions.FranchiseNotFoundException;
import com.pragma.franquicias.model.franchise.exceptions.InvalidFranchiseNameException;
import com.pragma.franquicias.model.franchise.gateways.RenameFranchiseGateway;
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
class RenameFranchiseUseCaseTest {

    private static final UUID FRANCHISE_ID = UUID.randomUUID();

    @Mock
    private RenameFranchiseGateway renameFranchiseGateway;

    private RenameFranchiseUseCase useCase() {
        return new RenameFranchiseUseCase(renameFranchiseGateway);
    }

    static String[] invalidNames() {
        return new String[] {null, "", "   ", "a".repeat(121)};
    }

    @Test
    void renamesWithTheTrimmedNameAndReturnsTheFranchise() {
        when(renameFranchiseGateway.rename(FRANCHISE_ID, "Pizza Co"))
                .thenReturn(Mono.just(Franchise.builder().id(FRANCHISE_ID).name("Pizza Co").build()));

        StepVerifier.create(useCase().execute(FRANCHISE_ID, "  Pizza Co  "))
                .assertNext(franchise -> assertEquals("Pizza Co", franchise.getName()))
                .verifyComplete();

        verify(renameFranchiseGateway).rename(FRANCHISE_ID, "Pizza Co");
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    void rejectsAnInvalidNameWithoutCallingTheGateway(String invalidName) {
        StepVerifier.create(useCase().execute(FRANCHISE_ID, invalidName))
                .expectError(InvalidFranchiseNameException.class)
                .verify();

        verifyNoInteractions(renameFranchiseGateway);
    }

    @Test
    void failsWithFranchiseNotFoundWhenTheGatewayFindsNoFranchise() {
        when(renameFranchiseGateway.rename(FRANCHISE_ID, "Pizza Co")).thenReturn(Mono.empty());

        StepVerifier.create(useCase().execute(FRANCHISE_ID, "Pizza Co"))
                .expectError(FranchiseNotFoundException.class)
                .verify();
    }

    @Test
    void propagatesTheDuplicateNameError() {
        when(renameFranchiseGateway.rename(FRANCHISE_ID, "Pizza Co"))
                .thenReturn(Mono.error(new DuplicateFranchiseNameException("Pizza Co")));

        StepVerifier.create(useCase().execute(FRANCHISE_ID, "Pizza Co"))
                .expectError(DuplicateFranchiseNameException.class)
                .verify();
    }
}
