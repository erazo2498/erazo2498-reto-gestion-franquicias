package com.pragma.franquicias.model.franchise.gateways;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface ExistsFranchiseGateway {

    Mono<Boolean> existsById(UUID franchiseId);
}
