package com.pragma.franquicias.usecase.createbranch;

import com.pragma.franquicias.model.branch.Branch;
import com.pragma.franquicias.model.branch.gateways.SaveBranchGateway;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CreateBranchUseCase {

    private final SaveBranchGateway saveBranchGateway;

    public Mono<Branch> execute(UUID franchiseId, String name) {
        return Mono.fromSupplier(() -> Branch.builder().id(UUID.randomUUID()).franchiseId(franchiseId).name(name).build())
                .flatMap(saveBranchGateway::save);
    }
}
