package com.pragma.franquicias.api.product;

import com.pragma.franquicias.model.product.Product;
import java.util.UUID;

public record ProductResponse(UUID id, UUID branchId, String name, int stock) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getBranchId(), product.getName(), product.getStock());
    }
}
