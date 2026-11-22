package com.pragma.franquicias.api;

import com.pragma.franquicias.support.PostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GetTopStockProductsEndToEndTest extends PostgresIntegrationTest {

    private static final String PATH = "/api/v1/franchises/{franchiseId}/top-stock-products";

    @Autowired
    private WebTestClient webTestClient;

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

    private UUID existingFranchise(String name) {
        UUID id = UUID.randomUUID();
        StepVerifier.create(databaseClient.sql("INSERT INTO franchise (id, name) VALUES (:id, :name)")
                        .bind("id", id)
                        .bind("name", name)
                        .then())
                .verifyComplete();
        return id;
    }

    private UUID existingBranch(UUID franchiseId, String name) {
        UUID id = UUID.randomUUID();
        StepVerifier.create(databaseClient.sql(
                                "INSERT INTO branch (id, franchise_id, name) VALUES (:id, :franchiseId, :name)")
                        .bind("id", id)
                        .bind("franchiseId", franchiseId)
                        .bind("name", name)
                        .then())
                .verifyComplete();
        return id;
    }

    private void existingProduct(UUID branchId, String name, int stock) {
        StepVerifier.create(databaseClient.sql(
                                "INSERT INTO product (id, branch_id, name, stock) VALUES (:id, :branchId, :name, :stock)")
                        .bind("id", UUID.randomUUID())
                        .bind("branchId", branchId)
                        .bind("name", name)
                        .bind("stock", stock)
                        .then())
                .verifyComplete();
    }

    @Test
    void returnsTheTopProductOfEachBranchIncludingAnEmptyBranch() {
        UUID franchiseId = existingFranchise("Burger Co");
        UUID centro = existingBranch(franchiseId, "Centro");
        UUID norte = existingBranch(franchiseId, "Norte");
        existingBranch(franchiseId, "Sur");
        existingProduct(centro, "Hamburguesa", 40);
        existingProduct(centro, "Gaseosa", 5);
        existingProduct(norte, "Pizza", 90);

        webTestClient.get().uri(PATH, franchiseId).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[0].branchName").isEqualTo("Centro")
                .jsonPath("$[0].product.name").isEqualTo("Hamburguesa")
                .jsonPath("$[0].product.stock").isEqualTo(40)
                .jsonPath("$[1].branchName").isEqualTo("Norte")
                .jsonPath("$[1].product.name").isEqualTo("Pizza")
                .jsonPath("$[2].branchName").isEqualTo("Sur")
                .jsonPath("$[2].product").isEmpty();
    }

    @Test
    void breaksATieByName() {
        UUID franchiseId = existingFranchise("Burger Co");
        UUID centro = existingBranch(franchiseId, "Centro");
        existingProduct(centro, "Zeta", 10);
        existingProduct(centro, "alfa", 10);

        webTestClient.get().uri(PATH, franchiseId).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].product.name").isEqualTo("alfa");
    }

    @Test
    void answersWithAnEmptyListWhenTheFranchiseHasNoBranches() {
        UUID franchiseId = existingFranchise("Burger Co");

        webTestClient.get().uri(PATH, franchiseId).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    void answersWith404WhenTheFranchiseDoesNotExist() {
        webTestClient.get().uri(PATH, UUID.randomUUID()).exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("FRANCHISE_NOT_FOUND");
    }

    @Test
    void answersWith400WhenTheFranchiseIdIsNotAUuid() {
        webTestClient.get().uri(PATH, "abc").exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");
    }
}
