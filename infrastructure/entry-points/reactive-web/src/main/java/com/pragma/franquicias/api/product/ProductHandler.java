package com.pragma.franquicias.api.product;

import com.pragma.franquicias.api.error.ApiErrorHandler;
import com.pragma.franquicias.api.error.InvalidPathIdException;
import com.pragma.franquicias.usecase.createproduct.CreateProductUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ProductHandler {

    private final CreateProductUseCase createProductUseCase;
    private final ApiErrorHandler apiErrorHandler;

    public Mono<ServerResponse> create(ServerRequest request) {
        return branchId(request)
                .flatMap(branchId -> request.bodyToMono(CreateProductRequest.class)
                        .defaultIfEmpty(new CreateProductRequest(null, null))
                        .flatMap(body -> createProductUseCase.execute(branchId, body.name(), body.stock())))
                .map(ProductResponse::from)
                .flatMap(response -> ServerResponse.status(HttpStatus.CREATED).bodyValue(response))
                .as(apiErrorHandler::handle);
    }

    private Mono<UUID> branchId(ServerRequest request) {
        return Mono.fromSupplier(() -> UUID.fromString(request.pathVariable("branchId")))
                .onErrorMap(
                        IllegalArgumentException.class,
                        error -> new InvalidPathIdException("Branch id must be a valid UUID"));
    }
}
