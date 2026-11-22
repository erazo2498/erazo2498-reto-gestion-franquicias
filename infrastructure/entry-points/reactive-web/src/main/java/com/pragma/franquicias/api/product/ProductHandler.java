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

    private static final String INVALID_BRANCH_ID = "Branch id must be a valid UUID";

    private final CreateProductUseCase createProductUseCase;
    private final ApiErrorHandler apiErrorHandler;

    public Mono<ServerResponse> create(ServerRequest request) {
        return pathUuid(request, "branchId", INVALID_BRANCH_ID)
                .flatMap(branchId -> request.bodyToMono(CreateProductRequest.class)
                        .defaultIfEmpty(new CreateProductRequest(null, null))
                        .flatMap(body -> createProductUseCase.execute(branchId, body.name(), body.stock())))
                .map(ProductResponse::from)
                .flatMap(response -> ServerResponse.status(HttpStatus.CREATED).bodyValue(response))
                .as(apiErrorHandler::handle);
    }

    // Reads a path variable as a UUID; anything else is a 400, not a 404.
    private Mono<UUID> pathUuid(ServerRequest request, String variable, String message) {
        return Mono.fromSupplier(() -> UUID.fromString(request.pathVariable(variable)))
                .onErrorMap(IllegalArgumentException.class, error -> new InvalidPathIdException(message));
    }
}
