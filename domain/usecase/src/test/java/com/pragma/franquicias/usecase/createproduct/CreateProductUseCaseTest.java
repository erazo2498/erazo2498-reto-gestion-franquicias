package com.pragma.franquicias.usecase.createproduct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pragma.franquicias.model.branch.exceptions.BranchNotFoundException;
import com.pragma.franquicias.model.product.Product;
import com.pragma.franquicias.model.product.exceptions.DuplicateProductNameException;
import com.pragma.franquicias.model.product.exceptions.InvalidProductNameException;
import com.pragma.franquicias.model.product.exceptions.InvalidProductStockException;
import com.pragma.franquicias.model.product.gateways.SaveProductGateway;
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
class CreateProductUseCaseTest {

    private static final String TOO_LONG_NAME = "a".repeat(121);
    private static final UUID BRANCH_ID = UUID.randomUUID();

    @Mock
    private SaveProductGateway saveProductGateway;

    private CreateProductUseCase useCase() {
        return new CreateProductUseCase(saveProductGateway);
    }

    private void gatewayReturnsWhatItReceives() {
        when(saveProductGateway.save(any(Product.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Product.class)));
    }

    @Test
    void createsProductWithTrimmedNameGeneratedIdTheGivenBranchAndStock() {
        gatewayReturnsWhatItReceives();

        StepVerifier.create(useCase().execute(BRANCH_ID, "  Hamburguesa  ", 10))
                .assertNext(product -> {
                    assertNotNull(product.getId());
                    assertEquals(BRANCH_ID, product.getBranchId());
                    assertEquals("Hamburguesa", product.getName());
                    assertEquals(10, product.getStock());
                })
                .verifyComplete();

        verify(saveProductGateway, times(1)).save(any(Product.class));
    }

    static String[] invalidNames() {
        return new String[] {null, "", "   ", TOO_LONG_NAME, "  " + TOO_LONG_NAME + "  "};
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    void rejectsNullBlankOrTooLongNameWithoutCallingTheGateway(String invalidName) {
        StepVerifier.create(useCase().execute(BRANCH_ID, invalidName, 10))
                .expectError(InvalidProductNameException.class)
                .verify();

        verifyNoInteractions(saveProductGateway);
    }

    static Integer[] invalidStocks() {
        return new Integer[] {null, -1};
    }

    @ParameterizedTest
    @MethodSource("invalidStocks")
    void rejectsAMissingOrNegativeStockWithoutCallingTheGateway(Integer invalidStock) {
        StepVerifier.create(useCase().execute(BRANCH_ID, "Hamburguesa", invalidStock))
                .expectError(InvalidProductStockException.class)
                .verify();

        verifyNoInteractions(saveProductGateway);
    }

    @Test
    void acceptsAStockOfZero() {
        gatewayReturnsWhatItReceives();

        StepVerifier.create(useCase().execute(BRANCH_ID, "Hamburguesa", 0))
                .assertNext(product -> assertEquals(0, product.getStock()))
                .verifyComplete();
    }

    @Test
    void rejectsAMissingBranchIdWithoutCallingTheGateway() {
        StepVerifier.create(useCase().execute(null, "Hamburguesa", 10))
                .expectError(NullPointerException.class)
                .verify();

        verifyNoInteractions(saveProductGateway);
    }

    @Test
    void acceptsTheMaximumNameLengthAfterTrim() {
        String maxName = "a".repeat(120);
        gatewayReturnsWhatItReceives();

        StepVerifier.create(useCase().execute(BRANCH_ID, " " + maxName + " ", 1))
                .assertNext(product -> assertEquals(maxName, product.getName()))
                .verifyComplete();
    }

    @Test
    void propagatesDuplicateNameError() {
        when(saveProductGateway.save(any(Product.class)))
                .thenReturn(Mono.error(new DuplicateProductNameException("Hamburguesa")));

        StepVerifier.create(useCase().execute(BRANCH_ID, "Hamburguesa", 10))
                .expectError(DuplicateProductNameException.class)
                .verify();
    }

    @Test
    void propagatesBranchNotFoundError() {
        when(saveProductGateway.save(any(Product.class)))
                .thenReturn(Mono.error(new BranchNotFoundException(BRANCH_ID)));

        StepVerifier.create(useCase().execute(BRANCH_ID, "Hamburguesa", 10))
                .expectError(BranchNotFoundException.class)
                .verify();
    }
}
