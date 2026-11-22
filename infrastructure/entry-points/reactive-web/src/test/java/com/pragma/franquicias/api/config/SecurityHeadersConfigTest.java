package com.pragma.franquicias.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@WebFluxTest
@ContextConfiguration(classes = SecurityHeadersConfigTest.PingRoute.class)
@Import(SecurityHeadersConfig.class)
class SecurityHeadersConfigTest {

    @Configuration
    static class PingRoute {
        @Bean
        RouterFunction<ServerResponse> ping() {
            return RouterFunctions.route().GET("/ping", request -> ServerResponse.ok().build()).build();
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void addsTheSecurityHeadersToEveryResponse() {
        webTestClient.get()
                .uri("/ping")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().doesNotExist("Server")
                .expectHeader().valueEquals("Cache-Control", "no-store")
                .expectHeader().valueEquals("Pragma", "no-cache")
                .expectHeader().valueEquals("Referrer-Policy", "strict-origin-when-cross-origin")
                .expectHeader().doesNotExist("Strict-Transport-Security");
    }
}
