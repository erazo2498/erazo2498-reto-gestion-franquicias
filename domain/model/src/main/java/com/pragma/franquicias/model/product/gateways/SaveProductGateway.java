package com.pragma.franquicias.model.product.gateways;

import com.pragma.franquicias.model.product.Product;
import reactor.core.publisher.Mono;

public interface SaveProductGateway {

    Mono<Product> save(Product product);
}
