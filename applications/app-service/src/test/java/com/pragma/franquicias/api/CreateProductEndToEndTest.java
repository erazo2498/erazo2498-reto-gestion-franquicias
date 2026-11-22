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
class CreateProductEndToEndTest extends PostgresIntegrationTest {

    private static final String PATH = "/api/v1/branches/{branchId}/products";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    @AfterEach
    void cleanTables() {
        // Children first: products reference branches and branches reference franchises. Cleaning also after each test
        // keeps other test classes unaffected.
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

    private WebTestClient.ResponseSpec post(Object branchId, String body) {
        return webTestClient
                .post()
                .uri(PATH, branchId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    @Test
    void createsAProductWithTheNameTrimmedAndStoresItForTheBranch() {
        UUID branchId = existingBranch("Burger Co", "Centro");

        post(branchId, "{\"name\":\"  Hamburguesa  \",\"stock\":10}")
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.branchId").isEqualTo(branchId.toString())
                .jsonPath("$.name").isEqualTo("Hamburguesa")
                .jsonPath("$.stock").isEqualTo(10);

        StepVerifier.create(databaseClient.sql("SELECT name, stock FROM product WHERE branch_id = :branchId")
                        .bind("branchId", branchId)
                        .map(row -> row.get("name", String.class) + "/" + row.get("stock", Integer.class))
                        .one())
                .assertNext(stored -> assertEquals("Hamburguesa/10", stored))
                .verifyComplete();
    }

    @Test
    void acceptsAStockOfZero() {
        UUID branchId = existingBranch("Burger Co", "Centro");

        post(branchId, "{\"name\":\"Hamburguesa\",\"stock\":0}")
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.stock").isEqualTo(0);
    }

    @Test
    void answersWith404WhenTheBranchDoesNotExist() {
        post(UUID.randomUUID(), "{\"name\":\"Hamburguesa\",\"stock\":10}")
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BRANCH_NOT_FOUND");
    }

    @Test
    void answersWith409WhenTheNameExistsInTheSameBranchIgnoringCase() {
        UUID branchId = existingBranch("Burger Co", "Centro");
        post(branchId, "{\"name\":\"Hamburguesa\",\"stock\":10}").expectStatus().isCreated();

        post(branchId, "{\"name\":\" HAMBURGUESA \",\"stock\":3}")
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("DUPLICATE_NAME");
    }

    @Test
    void allowsTheSameNameInAnotherBranch() {
        UUID firstBranch = existingBranch("Burger Co", "Centro");
        UUID secondBranch = existingBranch("Pizza Co", "Norte");
        post(firstBranch, "{\"name\":\"Hamburguesa\",\"stock\":10}").expectStatus().isCreated();

        post(secondBranch, "{\"name\":\"Hamburguesa\",\"stock\":5}").expectStatus().isCreated();
    }

    @Test
    void answersWith400WhenTheNameIsBlank() {
        UUID branchId = existingBranch("Burger Co", "Centro");

        post(branchId, "{\"name\":\"   \",\"stock\":10}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");
    }

    @Test
    void answersWith400WhenTheStockIsMissing() {
        UUID branchId = existingBranch("Burger Co", "Centro");

        post(branchId, "{\"name\":\"Hamburguesa\"}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");
    }

    // Goes through the real Jackson configuration of the application: none of these may be stored.
    @ParameterizedTest
    @ValueSource(strings = {"-1", "1.7", "\"5\"", "\"abc\"", "3000000000", "null"})
    void answersWith400AndStoresNothingWhenTheStockIsNotAValidWholeNumber(String stock) {
        UUID branchId = existingBranch("Burger Co", "Centro");

        post(branchId, "{\"name\":\"Hamburguesa\",\"stock\":" + stock + "}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");

        StepVerifier.create(databaseClient.sql("SELECT COUNT(*) AS total FROM product")
                        .map(row -> row.get("total", Long.class))
                        .one())
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void answersWith400WhenTheBranchIdIsNotAUuid() {
        post("abc", "{\"name\":\"Hamburguesa\",\"stock\":10}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");
    }
}
