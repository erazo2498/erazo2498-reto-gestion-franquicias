package com.pragma.franquicias.model.franchise.exceptions;

import com.pragma.franquicias.model.franchise.FranchiseValidator;

public class InvalidFranchiseNameException extends RuntimeException {

    public InvalidFranchiseNameException() {
        super("Franchise name must be between 1 and " + FranchiseValidator.MAX_NAME_LENGTH + " characters");
    }
}
