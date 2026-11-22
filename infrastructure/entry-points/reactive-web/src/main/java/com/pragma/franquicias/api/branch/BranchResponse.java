package com.pragma.franquicias.api.branch;

import com.pragma.franquicias.model.branch.Branch;
import java.util.UUID;

public record BranchResponse(UUID id, UUID franchiseId, String name) {

    public static BranchResponse from(Branch branch) {
        return new BranchResponse(branch.getId(), branch.getFranchiseId(), branch.getName());
    }
}
