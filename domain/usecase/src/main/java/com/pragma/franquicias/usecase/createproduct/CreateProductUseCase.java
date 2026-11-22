package com.pragma.franquicias.usecase.createproduct;

import com.pragma.franquicias.model.product.Product;
import com.pragma.franquicias.model.product.gateways.SaveProductGateway;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CreateProductUseCase {

    private final SaveProductGateway saveProductGateway;

    public Mono<Product> execute(UUID branchId, String name, Integer stock) {
        return Mono.fromSupplier(() ->
                        Product.builder().id(UUID.randomUUID()).branchId(branchId).name(name).stock(stock).build())
                .flatMap(saveProductGateway::save);
    }
}
