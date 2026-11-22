package com.pragma.franquicias.model.branch.exceptions;

import java.util.UUID;

public class BranchNotFoundException extends RuntimeException {

    public BranchNotFoundException(UUID branchId) {
        super("Branch '" + branchId + "' does not exist");
    }
}
