package com.pragma.franquicias.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
class DeleteProductEndToEndTest extends PostgresIntegrationTest {

    private static final String PATH = "/api/v1/branches/{branchId}/products/{productId}";

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

    private UUID existingBranch(String franchiseName, String branchName) {
        UUID franchiseId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        StepVerifier.create(databaseClient.sql("INSERT INTO franchise (id, name) VALUES (:id, :name)")
                        .bind("id", franchiseId)
                        .bind("name", franchiseName)
                        .then())
                .verifyComplete();
        StepVerifier.create(databaseClient.sql(
                                "INSERT INTO branch (id, franchise_id, name) VALUES (:id, :franchiseId, :name)")
                        .bind("id", branchId)
                        .bind("franchiseId", franchiseId)
                        .bind("name", branchName)
                        .then())
                .verifyComplete();
        return branchId;
    }

    private UUID existingProduct(UUID branchId, String name) {
        UUID productId = UUID.randomUUID();
        StepVerifier.create(databaseClient.sql(
                                "INSERT INTO product (id, branch_id, name, stock) VALUES (:id, :branchId, :name, 10)")
                        .bind("id", productId)
                        .bind("branchId", branchId)
                        .bind("name", name)
                        .then())
                .verifyComplete();
        return productId;
    }

    private WebTestClient.ResponseSpec delete(Object branchId, Object productId) {
        return webTestClient.delete().uri(PATH, branchId, productId).exchange();
    }

    private void assertProductCount(UUID branchId, long expected) {
        StepVerifier.create(databaseClient.sql("SELECT COUNT(*) AS total FROM product WHERE branch_id = :branchId")
                        .bind("branchId", branchId)
                        .map(row -> row.get("total", Long.class))
                        .one())
                .assertNext(total -> assertEquals(expected, total))
                .verifyComplete();
    }

    @Test
    void deletesTheProductWith204AndLeavesTheOthersOfTheBranch() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        UUID toDelete = existingProduct(branchId, "Hamburguesa");
        existingProduct(branchId, "Gaseosa");

        delete(branchId, toDelete).expectStatus().isNoContent().expectBody().isEmpty();

        assertProductCount(branchId, 1);
    }

    @Test
    void answersWith204WhenTheProductDoesNotExist() {
        UUID branchId = existingBranch("Burger Co", "Centro");

        delete(branchId, UUID.randomUUID()).expectStatus().isNoContent();
    }

    @Test
    void answersWith204AndKeepsTheProductWhenItBelongsToAnotherBranch() {
        UUID firstBranch = existingBranch("Burger Co", "Centro");
        UUID secondBranch = existingBranch("Pizza Co", "Norte");
        UUID productOfSecond = existingProduct(secondBranch, "Hamburguesa");

        delete(firstBranch, productOfSecond).expectStatus().isNoContent();

        assertProductCount(secondBranch, 1);
    }

    @Test
    void answersWith204WhenTheBranchDoesNotExist() {
        delete(UUID.randomUUID(), UUID.randomUUID()).expectStatus().isNoContent();
    }

    @Test
    void deletingTwiceGivesTheSameAnswerAndTheSameState() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        UUID productId = existingProduct(branchId, "Hamburguesa");

        delete(branchId, productId).expectStatus().isNoContent();
        delete(branchId, productId).expectStatus().isNoContent();

        assertProductCount(branchId, 0);
    }

    @Test
    void theNameCanBeCreatedAgainAfterTheProductIsDeleted() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        UUID productId = existingProduct(branchId, "Hamburguesa");

        delete(branchId, productId).expectStatus().isNoContent();

        webTestClient
                .post()
                .uri("/api/v1/branches/{branchId}/products", branchId)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Hamburguesa\",\"stock\":3}")
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void answersWith400AndTouchesNothingWhenAnIdIsNotAUuid() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        UUID productId = existingProduct(branchId, "Hamburguesa");

        delete("abc", productId)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA")
                .jsonPath("$.message").isEqualTo("Branch id must be a valid UUID");
        delete(branchId, "abc")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA")
                .jsonPath("$.message").isEqualTo("Product id must be a valid UUID");

        assertProductCount(branchId, 1);
    }
}
