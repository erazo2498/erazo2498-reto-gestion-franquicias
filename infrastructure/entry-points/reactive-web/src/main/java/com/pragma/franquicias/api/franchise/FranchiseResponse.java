package com.pragma.franquicias.api.franchise;

import com.pragma.franquicias.model.franchise.Franchise;
import java.util.UUID;

public record FranchiseResponse(UUID id, String name) {

    public static FranchiseResponse from(Franchise franchise) {
        return new FranchiseResponse(franchise.getId(), franchise.getName());
    }
}
