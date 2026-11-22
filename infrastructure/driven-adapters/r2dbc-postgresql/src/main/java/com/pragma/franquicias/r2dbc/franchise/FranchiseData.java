package com.pragma.franquicias.r2dbc.franchise;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("franchise")
public record FranchiseData(@Id UUID id, String name) {
}
