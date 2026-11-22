package com.pragma.franquicias.usecase.createfranchise;

import com.pragma.franquicias.model.franchise.Franchise;
import com.pragma.franquicias.model.franchise.gateways.SaveFranchiseGateway;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CreateFranchiseUseCase {

    private final SaveFranchiseGateway saveFranchiseGateway;

    public Mono<Franchise> execute(String name) {
        return Mono.fromSupplier(() -> Franchise.builder().id(UUID.randomUUID()).name(name).build())
                .flatMap(saveFranchiseGateway::save);
    }
}
