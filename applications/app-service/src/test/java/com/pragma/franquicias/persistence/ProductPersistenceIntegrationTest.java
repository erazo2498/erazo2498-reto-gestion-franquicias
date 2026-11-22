package com.pragma.franquicias.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pragma.franquicias.model.branch.Branch;
import com.pragma.franquicias.model.branch.exceptions.BranchNotFoundException;
import com.pragma.franquicias.model.branch.gateways.SaveBranchGateway;
import com.pragma.franquicias.model.franchise.Franchise;
import com.pragma.franquicias.model.franchise.gateways.SaveFranchiseGateway;
import com.pragma.franquicias.model.product.Product;
import com.pragma.franquicias.model.product.exceptions.DuplicateProductNameException;
import com.pragma.franquicias.model.product.gateways.SaveProductGateway;
import com.pragma.franquicias.support.PostgresIntegrationTest;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
class ProductPersistenceIntegrationTest extends PostgresIntegrationTest {

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

    private static Product product(UUID branchId, String name, int stock) {
        return Product.builder().id(UUID.randomUUID()).branchId(branchId).name(name).stock(stock).build();
    }

    @Test
    void savesTheProductAndStoresItInTheDatabase() {
        Product product = product(existingBranch("Burger Co", "Centro"), "Hamburguesa", 10);

        StepVerifier.create(saveProductGateway.save(product))
                .assertNext(saved -> assertEquals(product, saved))
                .verifyComplete();

        StepVerifier.create(databaseClient.sql("SELECT name, stock FROM product WHERE id = :id")
                        .bind("id", product.getId())
                        .map(row -> row.get("name", String.class) + "/" + row.get("stock", Integer.class))
                        .one())
                .expectNext("Hamburguesa/10")
                .verifyComplete();
    }

    @Test
    void storesAStockOfZero() {
        Product product = product(existingBranch("Burger Co", "Centro"), "Hamburguesa", 0);

        StepVerifier.create(saveProductGateway.save(product))
                .assertNext(saved -> assertEquals(0, saved.getStock()))
                .verifyComplete();
    }

    @Test
    void rejectsAProductOfABranchThatDoesNotExist() {
        StepVerifier.create(saveProductGateway.save(product(UUID.randomUUID(), "Hamburguesa", 10)))
                .expectError(BranchNotFoundException.class)
                .verify();
    }

    @Test
    void rejectsADuplicateNameInTheSameBranchIgnoringCase() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        Mono<Product> secondSave = saveProductGateway.save(product(branchId, "hamburguesa", 5));

        StepVerifier.create(saveProductGateway.save(product(branchId, "Hamburguesa", 10)).then(secondSave))
                .expectError(DuplicateProductNameException.class)
                .verify();
    }

    @Test
    void rejectsADuplicateNameWithAccentsAndEnyeIgnoringCase() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        Mono<Product> secondSave = saveProductGateway.save(product(branchId, "ÑANDÚ", 5));

        StepVerifier.create(saveProductGateway.save(product(branchId, "ñandú", 10)).then(secondSave))
                .expectError(DuplicateProductNameException.class)
                .verify();
    }

    @Test
    void allowsTheSameNameInDifferentBranches() {
        UUID firstBranch = existingBranch("Burger Co", "Centro");
        UUID secondBranch = existingBranch("Pizza Co", "Norte");
        Mono<Product> secondSave = saveProductGateway.save(product(secondBranch, "Hamburguesa", 5));

        StepVerifier.create(saveProductGateway.save(product(firstBranch, "Hamburguesa", 10)).then(secondSave))
                .assertNext(saved -> assertEquals(secondBranch, saved.getBranchId()))
                .verifyComplete();
    }

    @Test
    void onlyOneOfManySimultaneousCreationsWins() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        int attempts = 10;
        Flux<String> outcomes = Flux.range(0, attempts)
                .flatMap(
                        attempt -> saveProductGateway
                                .save(product(branchId, "Hamburguesa", 10))
                                .map(saved -> "CREATED")
                                .onErrorResume(error -> Mono.just(error.getClass().getSimpleName())),
                        attempts);

        StepVerifier.create(outcomes.collectList())
                .assertNext(results -> {
                    assertEquals(1, Collections.frequency(results, "CREATED"), results.toString());
                    assertEquals(
                            attempts - 1,
                            Collections.frequency(results, "DuplicateProductNameException"),
                            results.toString());
                })
                .verifyComplete();
    }

    @Test
    void databaseItselfRejectsNamesLongerThanTheLimit() {
        UUID branchId = existingBranch("Burger Co", "Centro");

        StepVerifier.create(insertRaw(branchId, "a".repeat(121), 10)).expectError().verify();
    }

    @Test
    void databaseItselfRejectsANegativeStock() {
        UUID branchId = existingBranch("Burger Co", "Centro");

        StepVerifier.create(insertRaw(branchId, "Hamburguesa", -1)).expectError().verify();
    }

    @Test
    void databaseRefusesToDeleteABranchThatStillHasProducts() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        StepVerifier.create(saveProductGateway.save(product(branchId, "Hamburguesa", 10)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(databaseClient.sql("DELETE FROM branch WHERE id = :id")
                        .bind("id", branchId)
                        .then())
                .expectError()
                .verify();
    }

    @Test
    void recordsTheMigrationInTheLiquibaseHistory() {
        StepVerifier.create(databaseClient.sql(
                                "SELECT COUNT(*) AS total FROM databasechangelog WHERE id = '003-create-product'")
                        .map(row -> row.get("total", Long.class))
                        .one())
                .expectNext(1L)
                .verifyComplete();
    }

    private Mono<Void> insertRaw(UUID branchId, String name, int stock) {
        return databaseClient
                .sql("INSERT INTO product (id, branch_id, name, stock) VALUES (:id, :branchId, :name, :stock)")
                .bind("id", UUID.randomUUID())
                .bind("branchId", branchId)
                .bind("name", name)
                .bind("stock", stock)
                .then();
    }
}
