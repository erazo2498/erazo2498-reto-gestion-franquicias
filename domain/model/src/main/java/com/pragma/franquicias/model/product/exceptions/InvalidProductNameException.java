package com.pragma.franquicias.model.product.exceptions;

import com.pragma.franquicias.model.product.ProductValidator;

public class InvalidProductNameException extends RuntimeException {

    public InvalidProductNameException() {
        super("Product name must be between 1 and " + ProductValidator.MAX_NAME_LENGTH + " characters");
    }
}
