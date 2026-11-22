package com.pragma.franquicias.usecase.deleteproduct;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pragma.franquicias.model.product.gateways.DeleteProductGateway;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class DeleteProductUseCaseTest {

    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Mock
    private DeleteProductGateway deleteProductGateway;

    private DeleteProductUseCase useCase() {
        return new DeleteProductUseCase(deleteProductGateway);
    }

    @Test
    void deletesTheProductOfTheBranchThroughTheGatewayExactlyOnce() {
        when(deleteProductGateway.delete(BRANCH_ID, PRODUCT_ID)).thenReturn(Mono.empty());

        StepVerifier.create(useCase().execute(BRANCH_ID, PRODUCT_ID)).verifyComplete();

        verify(deleteProductGateway, times(1)).delete(BRANCH_ID, PRODUCT_ID);
    }

    @Test
    void completesWithoutAnErrorWhateverTheGatewayFoundToDelete() {
        // The gateway completes empty both when it deleted a row and when there was nothing to delete.
        when(deleteProductGateway.delete(BRANCH_ID, PRODUCT_ID)).thenReturn(Mono.empty());

        StepVerifier.create(useCase().execute(BRANCH_ID, PRODUCT_ID)).expectComplete().verify();
    }

    @Test
    void propagatesGatewayErrors() {
        when(deleteProductGateway.delete(BRANCH_ID, PRODUCT_ID))
                .thenReturn(Mono.error(new IllegalStateException("connection lost")));

        StepVerifier.create(useCase().execute(BRANCH_ID, PRODUCT_ID))
                .expectError(IllegalStateException.class)
                .verify();
    }
}
