package com.pragma.franquicias.model.branch.gateways;

import com.pragma.franquicias.model.branch.Branch;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface RenameBranchGateway {

    // Renames the branch only if it belongs to the franchise. Completes empty when there is no such branch.
    Mono<Branch> rename(UUID franchiseId, UUID branchId, String name);
}
