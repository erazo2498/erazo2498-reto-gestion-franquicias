package com.pragma.franquicias.model.franchise.exceptions;

public class InvalidFranchiseNameException extends RuntimeException {

    public static final int MAX_LENGTH = 120;

    public InvalidFranchiseNameException() {
        super("Franchise name must be between 1 and " + MAX_LENGTH + " characters");
    }
}
