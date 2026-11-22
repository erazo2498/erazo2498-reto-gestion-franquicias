package com.pragma.franquicias.r2dbc.branch;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("branch")
public record BranchData(@Id UUID id, @Column("franchise_id") UUID franchiseId, String name) {
}
