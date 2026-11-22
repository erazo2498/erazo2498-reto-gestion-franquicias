package com.pragma.franquicias.api.product;

import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.PATCH;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class ProductRouter {

    @Bean
    public RouterFunction<ServerResponse> productRoutes(ProductHandler handler) {
        return route(POST("/api/v1/branches/{branchId}/products"), handler::create)
                .andRoute(DELETE("/api/v1/branches/{branchId}/products/{productId}"), handler::delete)
                .andRoute(PATCH("/api/v1/branches/{branchId}/products/{productId}/stock"), handler::updateStock);
    }
}
