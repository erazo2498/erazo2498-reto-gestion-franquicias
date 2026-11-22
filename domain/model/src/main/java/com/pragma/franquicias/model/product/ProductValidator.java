package com.pragma.franquicias.model.product;

import com.pragma.franquicias.model.product.exceptions.InvalidProductNameException;
import com.pragma.franquicias.model.product.exceptions.InvalidProductStockException;
import java.util.Optional;

public final class ProductValidator {

    public static final int MAX_NAME_LENGTH = 120;

    private ProductValidator() {
    }

    public static String validName(String name) {
        return Optional.ofNullable(name)
                .map(String::strip)
                .filter(stripped -> !stripped.isEmpty() && stripped.length() <= MAX_NAME_LENGTH)
                .orElseThrow(InvalidProductNameException::new);
    }

    public static int validStock(Integer stock) {
        return Optional.ofNullable(stock)
                .filter(value -> value >= 0)
                .orElseThrow(InvalidProductStockException::new);
    }
}
