package com.pragma.franquicias.api.rename;

import com.pragma.franquicias.api.branch.BranchResponse;
import com.pragma.franquicias.api.error.ApiErrorHandler;
import com.pragma.franquicias.api.error.InvalidPathIdException;
import com.pragma.franquicias.api.franchise.FranchiseResponse;
import com.pragma.franquicias.api.product.ProductResponse;
import com.pragma.franquicias.usecase.renamebranch.RenameBranchUseCase;
import com.pragma.franquicias.usecase.renamefranchise.RenameFranchiseUseCase;
import com.pragma.franquicias.usecase.renameproduct.RenameProductUseCase;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RenameHandler {

    private static final String INVALID_FRANCHISE_ID = "Franchise id must be a valid UUID";
    private static final String INVALID_BRANCH_ID = "Branch id must be a valid UUID";
    private static final String INVALID_PRODUCT_ID = "Product id must be a valid UUID";

    private final RenameFranchiseUseCase renameFranchiseUseCase;
    private final RenameBranchUseCase renameBranchUseCase;
    private final RenameProductUseCase renameProductUseCase;
    private final ApiErrorHandler apiErrorHandler;

    public Mono<ServerResponse> renameFranchise(ServerRequest request) {
        return pathUuid(request, "franchiseId", INVALID_FRANCHISE_ID)
                .flatMap(franchiseId -> newName(request)
                        .flatMap(name -> renameFranchiseUseCase.execute(franchiseId, name)))
                .map(FranchiseResponse::from)
                .flatMap(response -> ServerResponse.ok().bodyValue(response))
                .as(apiErrorHandler::handle);
    }

    public Mono<ServerResponse> renameBranch(ServerRequest request) {
        return pathUuid(request, "franchiseId", INVALID_FRANCHISE_ID)
                .zipWith(pathUuid(request, "branchId", INVALID_BRANCH_ID))
                .flatMap(ids -> newName(request)
                        .flatMap(name -> renameBranchUseCase.execute(ids.getT1(), ids.getT2(), name)))
                .map(BranchResponse::from)
                .flatMap(response -> ServerResponse.ok().bodyValue(response))
                .as(apiErrorHandler::handle);
    }

    public Mono<ServerResponse> renameProduct(ServerRequest request) {
        return pathUuid(request, "branchId", INVALID_BRANCH_ID)
                .zipWith(pathUuid(request, "productId", INVALID_PRODUCT_ID))
                .flatMap(ids -> newName(request)
                        .flatMap(name -> renameProductUseCase.execute(ids.getT1(), ids.getT2(), name)))
                .map(ProductResponse::from)
                .flatMap(response -> ServerResponse.ok().bodyValue(response))
                .as(apiErrorHandler::handle);
    }

    // A missing body or name reaches the use case as an empty text, which it rejects as an invalid name.
    private Mono<String> newName(ServerRequest request) {
        return request.bodyToMono(RenameRequest.class)
                .defaultIfEmpty(new RenameRequest(null))
                .map(body -> Optional.ofNullable(body.name()).orElse(""));
    }

    private Mono<UUID> pathUuid(ServerRequest request, String variable, String message) {
        return Mono.fromSupplier(() -> UUID.fromString(request.pathVariable(variable)))
                .onErrorMap(IllegalArgumentException.class, error -> new InvalidPathIdException(message));
    }
}
