package com.pragma.franquicias.api.branch;

import com.pragma.franquicias.api.error.ApiErrorHandler;
import com.pragma.franquicias.api.error.InvalidPathIdException;
import com.pragma.franquicias.usecase.createbranch.CreateBranchUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BranchHandler {

    private final CreateBranchUseCase createBranchUseCase;
    private final ApiErrorHandler apiErrorHandler;

    public Mono<ServerResponse> create(ServerRequest request) {
        return franchiseId(request)
                .flatMap(franchiseId -> request.bodyToMono(CreateBranchRequest.class)
                        .defaultIfEmpty(new CreateBranchRequest(null))
                        .flatMap(body -> createBranchUseCase.execute(franchiseId, body.name())))
                .map(BranchResponse::from)
                .flatMap(response -> ServerResponse.status(HttpStatus.CREATED).bodyValue(response))
                .as(apiErrorHandler::handle);
    }

    private Mono<UUID> franchiseId(ServerRequest request) {
        return Mono.fromSupplier(() -> UUID.fromString(request.pathVariable("franchiseId")))
                .onErrorMap(
                        IllegalArgumentException.class,
                        error -> new InvalidPathIdException("Franchise id must be a valid UUID"));
    }
}
