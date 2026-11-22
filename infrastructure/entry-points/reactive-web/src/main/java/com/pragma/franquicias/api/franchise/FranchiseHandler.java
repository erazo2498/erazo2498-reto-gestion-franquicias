package com.pragma.franquicias.api.franchise;

import com.pragma.franquicias.api.error.ApiErrorHandler;
import com.pragma.franquicias.usecase.createfranchise.CreateFranchiseUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class FranchiseHandler {

    private final CreateFranchiseUseCase createFranchiseUseCase;
    private final ApiErrorHandler apiErrorHandler;

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(CreateFranchiseRequest.class)
                .defaultIfEmpty(new CreateFranchiseRequest(null))
                .flatMap(body -> createFranchiseUseCase.execute(body.name()))
                .map(FranchiseResponse::from)
                .flatMap(response -> ServerResponse.status(HttpStatus.CREATED).bodyValue(response))
                .as(apiErrorHandler::handle);
    }
}
