package com.pragma.franquicias.model.branch;

import com.pragma.franquicias.model.product.Product;
import java.util.Optional;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

// A branch together with its product of highest stock; the product is empty when the branch has none.
@Value
@Builder
public class BranchTopProduct {
    UUID branchId;
    String branchName;
    Optional<Product> product;
}
