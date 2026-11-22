package com.pragma.franquicias.model.product.gateways;

import com.pragma.franquicias.model.product.Product;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface RenameProductGateway {

    // Renames the product only if it belongs to the branch. Completes empty when there is no such product.
    Mono<Product> rename(UUID branchId, UUID productId, String name);
}
