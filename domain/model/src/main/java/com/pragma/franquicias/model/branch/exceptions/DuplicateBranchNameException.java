package com.pragma.franquicias.model.branch.exceptions;

public class DuplicateBranchNameException extends RuntimeException {

    public DuplicateBranchNameException(String name) {
        super("A branch named '" + name + "' already exists in this franchise");
    }
}
