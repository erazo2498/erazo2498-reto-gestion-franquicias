package com.pragma.franquicias.model.product.exceptions;

public class InvalidProductStockException extends RuntimeException {

    public InvalidProductStockException() {
        super("Product stock is required and must be a whole number greater than or equal to 0");
    }
}
