package com.pragma.franquicias.usecase.createfranchise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pragma.franquicias.model.franchise.Franchise;
import com.pragma.franquicias.model.franchise.exceptions.DuplicateFranchiseNameException;
import com.pragma.franquicias.model.franchise.exceptions.InvalidFranchiseNameException;
import com.pragma.franquicias.model.franchise.gateways.SaveFranchiseGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CreateFranchiseUseCaseTest {

    private static final String TOO_LONG_NAME = "a".repeat(121);

    @Mock
    private SaveFranchiseGateway saveFranchiseGateway;

    private CreateFranchiseUseCase useCase() {
        return new CreateFranchiseUseCase(saveFranchiseGateway);
    }

    @Test
    void createsFranchiseWithTrimmedNameAndGeneratedId() {
        when(saveFranchiseGateway.save(any(Franchise.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Franchise.class)));

        StepVerifier.create(useCase().execute("  Burger Co  "))
                .assertNext(franchise -> {
                    assertNotNull(franchise.getId());
                    assertEquals("Burger Co", franchise.getName());
                })
                .verifyComplete();

        verify(saveFranchiseGateway, times(1)).save(any(Franchise.class));
    }

    static String[] invalidNames() {
        return new String[] {null, "", "   ", TOO_LONG_NAME, "  " + TOO_LONG_NAME + "  "};
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    void rejectsNullBlankOrTooLongNameWithoutCallingTheGateway(String invalidName) {
        StepVerifier.create(useCase().execute(invalidName))
                .expectError(InvalidFranchiseNameException.class)
                .verify();

        verifyNoInteractions(saveFranchiseGateway);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "  a  "})
    void acceptsTheMinimumLength(String name) {
        when(saveFranchiseGateway.save(any(Franchise.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Franchise.class)));

        StepVerifier.create(useCase().execute(name))
                .assertNext(franchise -> assertEquals("a", franchise.getName()))
                .verifyComplete();
    }

    @Test
    void acceptsTheMaximumLengthAfterTrim() {
        String maxName = "a".repeat(120);
        when(saveFranchiseGateway.save(any(Franchise.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Franchise.class)));

        StepVerifier.create(useCase().execute(" " + maxName + " "))
                .assertNext(franchise -> assertEquals(maxName, franchise.getName()))
                .verifyComplete();
    }

    @Test
    void propagatesDuplicateNameError() {
        when(saveFranchiseGateway.save(any(Franchise.class)))
                .thenReturn(Mono.error(new DuplicateFranchiseNameException("Burger Co")));

        StepVerifier.create(useCase().execute("Burger Co"))
                .expectError(DuplicateFranchiseNameException.class)
                .verify();
    }
}
