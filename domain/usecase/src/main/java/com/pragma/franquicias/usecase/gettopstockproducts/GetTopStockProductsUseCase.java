package com.pragma.franquicias.usecase.gettopstockproducts;

import com.pragma.franquicias.model.branch.BranchTopProduct;
import com.pragma.franquicias.model.franchise.exceptions.FranchiseNotFoundException;
import com.pragma.franquicias.model.franchise.gateways.ExistsFranchiseGateway;
import com.pragma.franquicias.model.product.gateways.FindTopStockProductsGateway;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class GetTopStockProductsUseCase {

    private final ExistsFranchiseGateway existsFranchiseGateway;
    private final FindTopStockProductsGateway findTopStockProductsGateway;

    // An empty list cannot tell "the franchise does not exist" from "it has no branches", so existence is checked first.
    public Flux<BranchTopProduct> execute(UUID franchiseId) {
        return existsFranchiseGateway
                .existsById(franchiseId)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(() -> new FranchiseNotFoundException(franchiseId)))
                .flatMapMany(exists -> findTopStockProductsGateway.findByFranchise(franchiseId));
    }
}
