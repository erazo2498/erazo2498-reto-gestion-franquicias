package com.pragma.franquicias.usecase.renamebranch;

import com.pragma.franquicias.model.branch.Branch;
import com.pragma.franquicias.model.branch.BranchValidator;
import com.pragma.franquicias.model.branch.exceptions.BranchNotFoundException;
import com.pragma.franquicias.model.branch.gateways.RenameBranchGateway;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class RenameBranchUseCase {

    private final RenameBranchGateway renameBranchGateway;

    public Mono<Branch> execute(UUID franchiseId, UUID branchId, String name) {
        return Mono.fromSupplier(() -> BranchValidator.validName(name))
                .flatMap(validName -> renameBranchGateway.rename(franchiseId, branchId, validName))
                .switchIfEmpty(Mono.error(() -> new BranchNotFoundException(branchId)));
    }
}
