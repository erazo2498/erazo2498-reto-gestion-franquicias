package com.pragma.franquicias.model.product.gateways;

import com.pragma.franquicias.model.product.Product;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface UpdateProductStockGateway {

    // Sets the stock of the product only if it belongs to the branch. Completes empty when there is no such product.
    Mono<Product> updateStock(UUID branchId, UUID productId, int stock);
}
