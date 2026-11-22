package com.pragma.franquicias.api.docs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI franchiseOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Franchise Management API")
                        .version("v1")
                        .description("Reactive REST API to manage franchises, their branches and their products."));
    }
}
