package com.pragma.franquicias.model.franchise;

import java.util.Objects;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
public class Franchise {
    UUID id;
    String name;

    @Builder(toBuilder = true)
    private Franchise(UUID id, String name) {
        this.id = Objects.requireNonNull(id, "Franchise id is required");
        this.name = FranchiseValidator.validName(name);
    }
}
