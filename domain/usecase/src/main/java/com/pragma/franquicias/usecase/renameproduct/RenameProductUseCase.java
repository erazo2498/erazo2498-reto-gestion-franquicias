package com.pragma.franquicias.usecase.renameproduct;

import com.pragma.franquicias.model.product.Product;
import com.pragma.franquicias.model.product.ProductValidator;
import com.pragma.franquicias.model.product.exceptions.ProductNotFoundException;
import com.pragma.franquicias.model.product.gateways.RenameProductGateway;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class RenameProductUseCase {

    private final RenameProductGateway renameProductGateway;

    public Mono<Product> execute(UUID branchId, UUID productId, String name) {
        return Mono.fromSupplier(() -> ProductValidator.validName(name))
                .flatMap(validName -> renameProductGateway.rename(branchId, productId, validName))
                .switchIfEmpty(Mono.error(() -> new ProductNotFoundException(branchId, productId)));
    }
}
