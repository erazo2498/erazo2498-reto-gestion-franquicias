package com.pragma.franquicias.model.branch;

import java.util.Objects;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
public class Branch {
    UUID id;
    UUID franchiseId;
    String name;

    @Builder(toBuilder = true)
    private Branch(UUID id, UUID franchiseId, String name) {
        this.id = Objects.requireNonNull(id, "Branch id is required");
        this.franchiseId = Objects.requireNonNull(franchiseId, "Branch franchise id is required");
        this.name = BranchValidator.validName(name);
    }
}
