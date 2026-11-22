package com.pragma.franquicias.model.branch.gateways;

import com.pragma.franquicias.model.branch.Branch;
import reactor.core.publisher.Mono;

public interface SaveBranchGateway {

    Mono<Branch> save(Branch branch);
}
