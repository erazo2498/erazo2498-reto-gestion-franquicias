package com.pragma.franquicias.api.product;

// The stock is an Integer, not an int: a missing stock must arrive as null (and be rejected), not as 0.
public record CreateProductRequest(String name, Integer stock) {
}
