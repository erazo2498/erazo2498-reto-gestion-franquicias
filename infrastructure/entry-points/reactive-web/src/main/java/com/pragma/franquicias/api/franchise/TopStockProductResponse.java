package com.pragma.franquicias.api.franchise;

import com.pragma.franquicias.model.branch.BranchTopProduct;
import com.pragma.franquicias.model.product.Product;
import java.util.UUID;

// A branch with its product of highest stock; the product is null when the branch has none.
public record TopStockProductResponse(UUID branchId, String branchName, TopProduct product) {

    public record TopProduct(UUID id, String name, int stock) {

        static TopProduct from(Product product) {
            return new TopProduct(product.getId(), product.getName(), product.getStock());
        }
    }

    public static TopStockProductResponse from(BranchTopProduct row) {
        return new TopStockProductResponse(
                row.getBranchId(), row.getBranchName(), row.getProduct().map(TopProduct::from).orElse(null));
    }
}
