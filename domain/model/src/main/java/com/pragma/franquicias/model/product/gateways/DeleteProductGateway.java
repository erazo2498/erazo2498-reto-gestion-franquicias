package com.pragma.franquicias.model.product.gateways;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface DeleteProductGateway {

    // Deletes the product only if it belongs to the branch. It completes empty whether a row was deleted or not.
    Mono<Void> delete(UUID branchId, UUID productId);
}
