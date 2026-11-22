package com.pragma.franquicias.api.product;

// An Integer, not an int: a missing stock must arrive as null (and be rejected), not as 0.
public record UpdateProductStockRequest(Integer stock) {
}
