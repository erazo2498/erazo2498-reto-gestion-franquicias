package com.pragma.franquicias.usecase.deleteproduct;

import com.pragma.franquicias.model.product.gateways.DeleteProductGateway;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class DeleteProductUseCase {

    private final DeleteProductGateway deleteProductGateway;

    // Deleting is idempotent: a product that is already gone is not an error.
    public Mono<Void> execute(UUID branchId, UUID productId) {
        return deleteProductGateway.delete(branchId, productId);
    }
}
