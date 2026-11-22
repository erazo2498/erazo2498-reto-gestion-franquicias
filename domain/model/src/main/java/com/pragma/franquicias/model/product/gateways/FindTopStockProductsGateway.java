package com.pragma.franquicias.model.product.gateways;

import com.pragma.franquicias.model.branch.BranchTopProduct;
import java.util.UUID;
import reactor.core.publisher.Flux;

public interface FindTopStockProductsGateway {

    // One element per branch of the franchise, with the product of highest stock of that branch (if it has any).
    Flux<BranchTopProduct> findByFranchise(UUID franchiseId);
}
