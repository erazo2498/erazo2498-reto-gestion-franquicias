package com.pragma.franquicias.usecase.renameproduct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pragma.franquicias.model.product.Product;
import com.pragma.franquicias.model.product.exceptions.DuplicateProductNameException;
import com.pragma.franquicias.model.product.exceptions.InvalidProductNameException;
import com.pragma.franquicias.model.product.exceptions.ProductNotFoundException;
import com.pragma.franquicias.model.product.gateways.RenameProductGateway;
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
class RenameProductUseCaseTest {

    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Mock
    private RenameProductGateway renameProductGateway;

    private RenameProductUseCase useCase() {
        return new RenameProductUseCase(renameProductGateway);
    }

    static String[] invalidNames() {
        return new String[] {null, "", "   ", "a".repeat(121)};
    }

    @Test
    void renamesWithTheTrimmedNameAndReturnsTheProduct() {
        when(renameProductGateway.rename(BRANCH_ID, PRODUCT_ID, "Pizza"))
                .thenReturn(Mono.just(
                        Product.builder().id(PRODUCT_ID).branchId(BRANCH_ID).name("Pizza").stock(3).build()));

        StepVerifier.create(useCase().execute(BRANCH_ID, PRODUCT_ID, "  Pizza  "))
                .assertNext(product -> assertEquals("Pizza", product.getName()))
                .verifyComplete();

        verify(renameProductGateway).rename(BRANCH_ID, PRODUCT_ID, "Pizza");
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    void rejectsAnInvalidNameWithoutCallingTheGateway(String invalidName) {
        StepVerifier.create(useCase().execute(BRANCH_ID, PRODUCT_ID, invalidName))
                .expectError(InvalidProductNameException.class)
                .verify();

        verifyNoInteractions(renameProductGateway);
    }

    @Test
    void failsWithProductNotFoundWhenTheGatewayFindsNoProduct() {
        when(renameProductGateway.rename(BRANCH_ID, PRODUCT_ID, "Pizza")).thenReturn(Mono.empty());

        StepVerifier.create(useCase().execute(BRANCH_ID, PRODUCT_ID, "Pizza"))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    void propagatesTheDuplicateNameError() {
        when(renameProductGateway.rename(BRANCH_ID, PRODUCT_ID, "Pizza"))
                .thenReturn(Mono.error(new DuplicateProductNameException("Pizza")));

        StepVerifier.create(useCase().execute(BRANCH_ID, PRODUCT_ID, "Pizza"))
                .expectError(DuplicateProductNameException.class)
                .verify();
    }
}
