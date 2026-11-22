package com.pragma.franquicias.usecase.updateproductstock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pragma.franquicias.model.product.Product;
import com.pragma.franquicias.model.product.exceptions.InvalidProductStockException;
import com.pragma.franquicias.model.product.exceptions.ProductNotFoundException;
import com.pragma.franquicias.model.product.gateways.UpdateProductStockGateway;
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
class UpdateProductStockUseCaseTest {

    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Mock
    private UpdateProductStockGateway updateProductStockGateway;

    private UpdateProductStockUseCase useCase() {
        return new UpdateProductStockUseCase(updateProductStockGateway);
    }

    private static Product productWithStock(int stock) {
        return Product.builder().id(PRODUCT_ID).branchId(BRANCH_ID).name("Hamburguesa").stock(stock).build();
    }

    @Test
    void updatesTheStockThroughTheGatewayAndReturnsTheProduct() {
        when(updateProductStockGateway.updateStock(BRANCH_ID, PRODUCT_ID, 25))
                .thenReturn(Mono.just(productWithStock(25)));

        StepVerifier.create(useCase().execute(BRANCH_ID, PRODUCT_ID, 25))
                .assertNext(product -> assertEquals(25, product.getStock()))
                .verifyComplete();

        verify(updateProductStockGateway).updateStock(BRANCH_ID, PRODUCT_ID, 25);
    }

    @Test
    void acceptsAStockOfZero() {
        when(updateProductStockGateway.updateStock(BRANCH_ID, PRODUCT_ID, 0))
                .thenReturn(Mono.just(productWithStock(0)));

        StepVerifier.create(useCase().execute(BRANCH_ID, PRODUCT_ID, 0))
                .assertNext(product -> assertEquals(0, product.getStock()))
                .verifyComplete();
    }

    static Integer[] invalidStocks() {
        return new Integer[] {null, -1, Integer.MIN_VALUE};
    }

    @ParameterizedTest
    @MethodSource("invalidStocks")
    void rejectsAMissingOrNegativeStockWithoutCallingTheGateway(Integer invalidStock) {
        StepVerifier.create(useCase().execute(BRANCH_ID, PRODUCT_ID, invalidStock))
                .expectError(InvalidProductStockException.class)
                .verify();

        verifyNoInteractions(updateProductStockGateway);
    }

    @Test
    void failsWithProductNotFoundWhenTheGatewayFindsNoProduct() {
        when(updateProductStockGateway.updateStock(BRANCH_ID, PRODUCT_ID, 25)).thenReturn(Mono.empty());

        StepVerifier.create(useCase().execute(BRANCH_ID, PRODUCT_ID, 25))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    void propagatesGatewayErrors() {
        when(updateProductStockGateway.updateStock(BRANCH_ID, PRODUCT_ID, 25))
                .thenReturn(Mono.error(new IllegalStateException("connection lost")));

        StepVerifier.create(useCase().execute(BRANCH_ID, PRODUCT_ID, 25))
                .expectError(IllegalStateException.class)
                .verify();
    }
}
