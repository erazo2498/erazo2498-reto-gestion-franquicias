package com.pragma.franquicias.model.franchise.exceptions;

import java.util.UUID;

public class FranchiseNotFoundException extends RuntimeException {

    public FranchiseNotFoundException(UUID franchiseId) {
        super("Franchise '" + franchiseId + "' does not exist");
    }
}
