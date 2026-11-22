package com.pragma.franquicias.api.error;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ApiErrorHandler {

    public Mono<ServerResponse> handle(Mono<ServerResponse> response) {
        return response;
    }
}
