package com.pragma.franquicias.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pragma.franquicias.model.branch.Branch;
import com.pragma.franquicias.model.branch.gateways.SaveBranchGateway;
import com.pragma.franquicias.model.franchise.Franchise;
import com.pragma.franquicias.model.franchise.gateways.SaveFranchiseGateway;
import com.pragma.franquicias.model.product.Product;
import com.pragma.franquicias.model.product.gateways.DeleteProductGateway;
import com.pragma.franquicias.model.product.gateways.SaveProductGateway;
import com.pragma.franquicias.support.PostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SpringBootTest
class DeleteProductPersistenceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private DeleteProductGateway deleteProductGateway;

    @Autowired
    private SaveProductGateway saveProductGateway;

    @Autowired
    private SaveBranchGateway saveBranchGateway;

    @Autowired
    private SaveFranchiseGateway saveFranchiseGateway;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    @AfterEach
    void cleanTables() {
        // Children first: products reference branches and branches reference franchises.
        StepVerifier.create(databaseClient.sql("DELETE FROM product").then()).verifyComplete();
        StepVerifier.create(databaseClient.sql("DELETE FROM branch").then()).verifyComplete();
        StepVerifier.create(databaseClient.sql("DELETE FROM franchise").then()).verifyComplete();
    }

    private UUID existingBranch(String franchiseName, String branchName) {
        UUID franchiseId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        StepVerifier.create(saveFranchiseGateway.save(
                        Franchise.builder().id(franchiseId).name(franchiseName).build()))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(saveBranchGateway.save(
                        Branch.builder().id(branchId).franchiseId(franchiseId).name(branchName).build()))
                .expectNextCount(1)
                .verifyComplete();
        return branchId;
    }

    private UUID existingProduct(UUID branchId, String name) {
        UUID productId = UUID.randomUUID();
        StepVerifier.create(saveProductGateway.save(
                        Product.builder().id(productId).branchId(branchId).name(name).stock(10).build()))
                .expectNextCount(1)
                .verifyComplete();
        return productId;
    }

    private void assertProductCount(UUID branchId, long expected) {
        StepVerifier.create(databaseClient.sql("SELECT COUNT(*) AS total FROM product WHERE branch_id = :branchId")
                        .bind("branchId", branchId)
                        .map(row -> row.get("total", Long.class))
                        .one())
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    void deletesTheProductAndLeavesTheOthersOfTheBranchIntact() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        UUID toDelete = existingProduct(branchId, "Hamburguesa");
        existingProduct(branchId, "Gaseosa");

        StepVerifier.create(deleteProductGateway.delete(branchId, toDelete)).verifyComplete();

        assertProductCount(branchId, 1);
        StepVerifier.create(databaseClient.sql("SELECT name FROM product WHERE branch_id = :branchId")
                        .bind("branchId", branchId)
                        .map(row -> row.get("name", String.class))
                        .one())
                .expectNext("Gaseosa")
                .verifyComplete();
    }

    @Test
    void doesNotDeleteAProductThatBelongsToAnotherBranch() {
        UUID firstBranch = existingBranch("Burger Co", "Centro");
        UUID secondBranch = existingBranch("Pizza Co", "Norte");
        UUID productOfSecond = existingProduct(secondBranch, "Hamburguesa");

        StepVerifier.create(deleteProductGateway.delete(firstBranch, productOfSecond)).verifyComplete();

        assertProductCount(secondBranch, 1);
    }

    @Test
    void completesEmptyWhenTheProductDoesNotExist() {
        UUID branchId = existingBranch("Burger Co", "Centro");

        StepVerifier.create(deleteProductGateway.delete(branchId, UUID.randomUUID())).verifyComplete();
    }

    @Test
    void completesEmptyWhenTheBranchDoesNotExist() {
        StepVerifier.create(deleteProductGateway.delete(UUID.randomUUID(), UUID.randomUUID()))
                .verifyComplete();
    }

    @Test
    void deletingTwiceLeavesTheSameState() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        UUID productId = existingProduct(branchId, "Hamburguesa");

        StepVerifier.create(deleteProductGateway.delete(branchId, productId)
                        .then(deleteProductGateway.delete(branchId, productId)))
                .verifyComplete();

        assertProductCount(branchId, 0);
    }

    @Test
    void theNameCanBeReusedAfterTheProductIsDeleted() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        UUID productId = existingProduct(branchId, "Hamburguesa");

        StepVerifier.create(deleteProductGateway.delete(branchId, productId)).verifyComplete();
        existingProduct(branchId, "Hamburguesa");

        assertProductCount(branchId, 1);
    }

    @Test
    void manySimultaneousDeletionsOfTheSameProductAllCompleteAndItDisappearsOnce() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        UUID productId = existingProduct(branchId, "Hamburguesa");
        int attempts = 10;

        StepVerifier.create(Flux.range(0, attempts)
                        .flatMap(attempt -> deleteProductGateway.delete(branchId, productId), attempts)
                        .collectList())
                .assertNext(results -> assertEquals(0, results.size()))
                .verifyComplete();

        assertProductCount(branchId, 0);
    }
}
