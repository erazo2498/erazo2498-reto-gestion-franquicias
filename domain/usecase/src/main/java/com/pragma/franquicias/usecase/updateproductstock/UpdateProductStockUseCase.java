package com.pragma.franquicias.usecase.updateproductstock;

import com.pragma.franquicias.model.product.Product;
import com.pragma.franquicias.model.product.ProductValidator;
import com.pragma.franquicias.model.product.exceptions.ProductNotFoundException;
import com.pragma.franquicias.model.product.gateways.UpdateProductStockGateway;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class UpdateProductStockUseCase {

    private final UpdateProductStockGateway updateProductStockGateway;

    public Mono<Product> execute(UUID branchId, UUID productId, Integer stock) {
        return Mono.fromSupplier(() -> ProductValidator.validStock(stock))
                .flatMap(validStock -> updateProductStockGateway.updateStock(branchId, productId, validStock))
                .switchIfEmpty(Mono.error(() -> new ProductNotFoundException(branchId, productId)));
    }
}
