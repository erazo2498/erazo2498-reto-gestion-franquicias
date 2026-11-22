package com.pragma.franquicias.model.franchise.gateways;

import com.pragma.franquicias.model.franchise.Franchise;
import reactor.core.publisher.Mono;

public interface SaveFranchiseGateway {

    Mono<Franchise> save(Franchise franchise);
}
