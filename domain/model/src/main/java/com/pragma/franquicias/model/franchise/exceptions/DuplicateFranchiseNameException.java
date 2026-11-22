package com.pragma.franquicias.model.franchise.exceptions;

public class DuplicateFranchiseNameException extends RuntimeException {

    public DuplicateFranchiseNameException(String name) {
        super("A franchise named '" + name + "' already exists");
    }
}
