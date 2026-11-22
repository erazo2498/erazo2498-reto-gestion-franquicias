package com.pragma.franquicias.api.rename;

import static org.springframework.web.reactive.function.server.RequestPredicates.PATCH;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RenameRouter {

    @Bean
    public RouterFunction<ServerResponse> renameRoutes(RenameHandler handler) {
        return route(PATCH("/api/v1/franchises/{franchiseId}"), handler::renameFranchise)
                .andRoute(PATCH("/api/v1/franchises/{franchiseId}/branches/{branchId}"), handler::renameBranch)
                .andRoute(PATCH("/api/v1/branches/{branchId}/products/{productId}"), handler::renameProduct);
    }
}
