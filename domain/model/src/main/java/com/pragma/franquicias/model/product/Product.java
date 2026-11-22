package com.pragma.franquicias.model.product;

import java.util.Objects;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
public class Product {
    UUID id;
    UUID branchId;
    String name;
    int stock;

    // The builder takes an Integer so a missing stock can be told apart from a stock of 0.
    @Builder(toBuilder = true)
    private Product(UUID id, UUID branchId, String name, Integer stock) {
        this.id = Objects.requireNonNull(id, "Product id is required");
        this.branchId = Objects.requireNonNull(branchId, "Product branch id is required");
        this.name = ProductValidator.validName(name);
        this.stock = ProductValidator.validStock(stock);
    }
}
