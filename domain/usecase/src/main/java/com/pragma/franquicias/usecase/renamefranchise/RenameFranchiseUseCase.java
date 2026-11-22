package com.pragma.franquicias.usecase.renamefranchise;

import com.pragma.franquicias.model.franchise.Franchise;
import com.pragma.franquicias.model.franchise.FranchiseValidator;
import com.pragma.franquicias.model.franchise.exceptions.FranchiseNotFoundException;
import com.pragma.franquicias.model.franchise.gateways.RenameFranchiseGateway;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class RenameFranchiseUseCase {

    private final RenameFranchiseGateway renameFranchiseGateway;

    public Mono<Franchise> execute(UUID franchiseId, String name) {
        return Mono.fromSupplier(() -> FranchiseValidator.validName(name))
                .flatMap(validName -> renameFranchiseGateway.rename(franchiseId, validName))
                .switchIfEmpty(Mono.error(() -> new FranchiseNotFoundException(franchiseId)));
    }
}
