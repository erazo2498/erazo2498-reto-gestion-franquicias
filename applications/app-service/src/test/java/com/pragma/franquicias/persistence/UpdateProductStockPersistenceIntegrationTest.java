package com.pragma.franquicias.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pragma.franquicias.model.branch.Branch;
import com.pragma.franquicias.model.branch.gateways.SaveBranchGateway;
import com.pragma.franquicias.model.franchise.Franchise;
import com.pragma.franquicias.model.franchise.gateways.SaveFranchiseGateway;
import com.pragma.franquicias.model.product.Product;
import com.pragma.franquicias.model.product.gateways.SaveProductGateway;
import com.pragma.franquicias.model.product.gateways.UpdateProductStockGateway;
import com.pragma.franquicias.support.PostgresIntegrationTest;
import java.util.List;
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
class UpdateProductStockPersistenceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private UpdateProductStockGateway updateProductStockGateway;

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

    private UUID existingProduct(UUID branchId, String name, int stock) {
        UUID productId = UUID.randomUUID();
        StepVerifier.create(saveProductGateway.save(
                        Product.builder().id(productId).branchId(branchId).name(name).stock(stock).build()))
                .expectNextCount(1)
                .verifyComplete();
        return productId;
    }

    private void assertStoredStock(UUID productId, int expected) {
        StepVerifier.create(databaseClient.sql("SELECT stock FROM product WHERE id = :id")
                        .bind("id", productId)
                        .map(row -> row.get("stock", Integer.class))
                        .one())
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    void updatesTheStockAndKeepsTheRestOfTheProduct() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        UUID productId = existingProduct(branchId, "Hamburguesa", 10);

        StepVerifier.create(updateProductStockGateway.updateStock(branchId, productId, 25))
                .assertNext(updated -> {
                    assertEquals(productId, updated.getId());
                    assertEquals(branchId, updated.getBranchId());
                    assertEquals("Hamburguesa", updated.getName());
                    assertEquals(25, updated.getStock());
                })
                .verifyComplete();

        assertStoredStock(productId, 25);
    }

    @Test
    void acceptsAStockOfZero() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        UUID productId = existingProduct(branchId, "Hamburguesa", 10);

        StepVerifier.create(updateProductStockGateway.updateStock(branchId, productId, 0))
                .assertNext(updated -> assertEquals(0, updated.getStock()))
                .verifyComplete();
    }

    @Test
    void doesNotTouchAProductThatBelongsToAnotherBranch() {
        UUID firstBranch = existingBranch("Burger Co", "Centro");
        UUID secondBranch = existingBranch("Pizza Co", "Norte");
        UUID productOfSecond = existingProduct(secondBranch, "Hamburguesa", 10);

        StepVerifier.create(updateProductStockGateway.updateStock(firstBranch, productOfSecond, 99))
                .verifyComplete();

        assertStoredStock(productOfSecond, 10);
    }

    @Test
    void completesEmptyWhenTheProductDoesNotExist() {
        UUID branchId = existingBranch("Burger Co", "Centro");

        StepVerifier.create(updateProductStockGateway.updateStock(branchId, UUID.randomUUID(), 5))
                .verifyComplete();
    }

    @Test
    void completesEmptyWhenTheBranchDoesNotExist() {
        StepVerifier.create(updateProductStockGateway.updateStock(UUID.randomUUID(), UUID.randomUUID(), 5))
                .verifyComplete();
    }

    @Test
    void updatingTwiceWithTheSameValueLeavesTheSameState() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        UUID productId = existingProduct(branchId, "Hamburguesa", 10);

        StepVerifier.create(updateProductStockGateway.updateStock(branchId, productId, 25)
                        .then(updateProductStockGateway.updateStock(branchId, productId, 25)))
                .assertNext(updated -> assertEquals(25, updated.getStock()))
                .verifyComplete();

        assertStoredStock(productId, 25);
    }

    @Test
    void manySimultaneousUpdatesAllCompleteAndTheLastWriteWins() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        UUID productId = existingProduct(branchId, "Hamburguesa", 10);
        int attempts = 10;

        StepVerifier.create(Flux.range(1, attempts)
                        .flatMap(value -> updateProductStockGateway.updateStock(branchId, productId, value), attempts)
                        .collectList())
                .assertNext(results -> assertEquals(attempts, results.size()))
                .verifyComplete();

        StepVerifier.create(databaseClient.sql("SELECT stock FROM product WHERE id = :id")
                        .bind("id", productId)
                        .map(row -> row.get("stock", Integer.class))
                        .one())
                .assertNext(stock -> assertTrue(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).contains(stock), "stock: " + stock))
                .verifyComplete();
    }

    @Test
    void databaseItselfRejectsANegativeStock() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        UUID productId = existingProduct(branchId, "Hamburguesa", 10);

        StepVerifier.create(databaseClient.sql("UPDATE product SET stock = -1 WHERE id = :id")
                        .bind("id", productId)
                        .then())
                .expectError()
                .verify();

        assertStoredStock(productId, 10);
    }
}
