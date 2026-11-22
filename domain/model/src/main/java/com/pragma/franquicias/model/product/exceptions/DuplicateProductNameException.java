package com.pragma.franquicias.model.product.exceptions;

public class DuplicateProductNameException extends RuntimeException {

    public DuplicateProductNameException(String name) {
        super("A product named '" + name + "' already exists in this branch");
    }
}
