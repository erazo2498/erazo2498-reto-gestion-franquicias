package com.pragma.franquicias.model.product.exceptions;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(UUID branchId, UUID productId) {
        super("Product '" + productId + "' was not found in branch '" + branchId + "'");
    }
}
