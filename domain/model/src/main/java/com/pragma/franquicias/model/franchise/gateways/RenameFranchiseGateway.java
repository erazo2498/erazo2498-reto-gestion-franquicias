package com.pragma.franquicias.model.franchise.gateways;

import com.pragma.franquicias.model.franchise.Franchise;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface RenameFranchiseGateway {

    // Completes empty when there is no such franchise; fails with a duplicate-name error if the name is taken.
    Mono<Franchise> rename(UUID franchiseId, String name);
}
