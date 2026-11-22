package com.pragma.franquicias.model.branch.exceptions;

import com.pragma.franquicias.model.branch.BranchValidator;

public class InvalidBranchNameException extends RuntimeException {

    public InvalidBranchNameException() {
        super("Branch name must be between 1 and " + BranchValidator.MAX_NAME_LENGTH + " characters");
    }
}
