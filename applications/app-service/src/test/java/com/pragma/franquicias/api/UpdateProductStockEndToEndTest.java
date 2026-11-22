package com.pragma.franquicias.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pragma.franquicias.support.PostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class UpdateProductStockEndToEndTest extends PostgresIntegrationTest {

    private static final String PATH = "/api/v1/branches/{branchId}/products/{productId}/stock";

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

    private UUID existingProduct(UUID branchId, String name, int stock) {
        UUID productId = UUID.randomUUID();
        StepVerifier.create(databaseClient.sql(
                                "INSERT INTO product (id, branch_id, name, stock) VALUES (:id, :branchId, :name, :stock)")
                        .bind("id", productId)
                        .bind("branchId", branchId)
                        .bind("name", name)
                        .bind("stock", stock)
                        .then())
                .verifyComplete();
        return productId;
    }

    private WebTestClient.ResponseSpec patch(Object branchId, Object productId, String body) {
        return webTestClient
                .patch()
                .uri(PATH, branchId, productId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    private void assertStoredStock(UUID productId, int expected) {
        StepVerifier.create(databaseClient.sql("SELECT stock FROM product WHERE id = :id")
                        .bind("id", productId)
                        .map(row -> row.get("stock", Integer.class))
                        .one())
                .assertNext(stock -> assertEquals(expected, stock))
                .verifyComplete();
    }

    @Test
    void updatesTheStockAnswersWith200AndKeepsTheRestOfTheProduct() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        UUID productId = existingProduct(branchId, "Hamburguesa", 10);

        patch(branchId, productId, "{\"stock\":25}")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(productId.toString())
                .jsonPath("$.branchId").isEqualTo(branchId.toString())
                .jsonPath("$.name").isEqualTo("Hamburguesa")
                .jsonPath("$.stock").isEqualTo(25);

        assertStoredStock(productId, 25);
    }

    @Test
    void acceptsAStockOfZero() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        UUID productId = existingProduct(branchId, "Hamburguesa", 10);

        patch(branchId, productId, "{\"stock\":0}").expectStatus().isOk().expectBody().jsonPath("$.stock").isEqualTo(0);

        assertStoredStock(productId, 0);
    }

    @Test
    void sendingTheSameValueTwiceGivesTheSameAnswerAndState() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        UUID productId = existingProduct(branchId, "Hamburguesa", 10);

        patch(branchId, productId, "{\"stock\":25}").expectStatus().isOk();
        patch(branchId, productId, "{\"stock\":25}").expectStatus().isOk();

        assertStoredStock(productId, 25);
    }

    @Test
    void answersWith404WhenTheProductDoesNotExist() {
        UUID branchId = existingBranch("Burger Co", "Centro");

        patch(branchId, UUID.randomUUID(), "{\"stock\":25}")
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    void answersWith404AndKeepsTheProductWhenItBelongsToAnotherBranch() {
        UUID firstBranch = existingBranch("Burger Co", "Centro");
        UUID secondBranch = existingBranch("Pizza Co", "Norte");
        UUID productOfSecond = existingProduct(secondBranch, "Hamburguesa", 10);

        patch(firstBranch, productOfSecond, "{\"stock\":99}")
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("PRODUCT_NOT_FOUND");

        assertStoredStock(productOfSecond, 10);
    }

    @Test
    void answersWith404WhenTheBranchDoesNotExist() {
        patch(UUID.randomUUID(), UUID.randomUUID(), "{\"stock\":25}")
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("PRODUCT_NOT_FOUND");
    }

    // Goes through the real Jackson configuration of the application: none of these may change the stored stock.
    @ParameterizedTest
    @ValueSource(strings = {"-1", "1.7", "\"5\"", "\"abc\"", "3000000000", "null", "true"})
    void answersWith400AndKeepsTheStockWhenItIsNotAValidWholeNumber(String stock) {
        UUID branchId = existingBranch("Burger Co", "Centro");
        UUID productId = existingProduct(branchId, "Hamburguesa", 10);

        patch(branchId, productId, "{\"stock\":" + stock + "}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");

        assertStoredStock(productId, 10);
    }

    @Test
    void answersWith400AndKeepsTheStockWhenItIsMissing() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        UUID productId = existingProduct(branchId, "Hamburguesa", 10);

        patch(branchId, productId, "{}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");

        assertStoredStock(productId, 10);
    }

    @Test
    void answersWith400WhenAnIdIsNotAUuid() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        UUID productId = existingProduct(branchId, "Hamburguesa", 10);

        patch("abc", productId, "{\"stock\":25}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Branch id must be a valid UUID");
        patch(branchId, "abc", "{\"stock\":25}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Product id must be a valid UUID");

        assertStoredStock(productId, 10);
    }
}
