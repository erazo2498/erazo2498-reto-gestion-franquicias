package com.pragma.franquicias.r2dbc.product;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("product")
public record ProductData(@Id UUID id, @Column("branch_id") UUID branchId, String name, int stock) {
}
