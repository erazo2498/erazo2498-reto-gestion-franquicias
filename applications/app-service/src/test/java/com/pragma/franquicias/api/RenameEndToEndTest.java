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
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RenameEndToEndTest extends PostgresIntegrationTest {

    private static final String FRANCHISE_PATH = "/api/v1/franchises/{franchiseId}";
    private static final String BRANCH_PATH = "/api/v1/franchises/{franchiseId}/branches/{branchId}";
    private static final String PRODUCT_PATH = "/api/v1/branches/{branchId}/products/{productId}";

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

    private UUID franchise(String name) {
        UUID id = UUID.randomUUID();
        StepVerifier.create(databaseClient.sql("INSERT INTO franchise (id, name) VALUES (:id, :name)")
                        .bind("id", id)
                        .bind("name", name)
                        .then())
                .verifyComplete();
        return id;
    }

    private UUID branch(UUID franchiseId, String name) {
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

    private UUID product(UUID branchId, String name) {
        UUID id = UUID.randomUUID();
        StepVerifier.create(databaseClient.sql(
                                "INSERT INTO product (id, branch_id, name, stock) VALUES (:id, :branchId, :name, 10)")
                        .bind("id", id)
                        .bind("branchId", branchId)
                        .bind("name", name)
                        .then())
                .verifyComplete();
        return id;
    }

    private WebTestClient.ResponseSpec patch(String path, String body, Object... ids) {
        return webTestClient
                .patch()
                .uri(path, ids)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    private void assertStoredName(String table, UUID id, String expected) {
        StepVerifier.create(databaseClient.sql("SELECT name FROM " + table + " WHERE id = :id")
                        .bind("id", id)
                        .map(row -> row.get("name", String.class))
                        .one())
                .assertNext(name -> assertEquals(expected, name))
                .verifyComplete();
    }

    // ---- franchise

    @Test
    void renamesAFranchiseWithTheNameTrimmed() {
        UUID id = franchise("Burger Co");

        patch(FRANCHISE_PATH, "{\"name\":\"  Pizza Co  \"}", id)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id.toString())
                .jsonPath("$.name").isEqualTo("Pizza Co");

        assertStoredName("franchise", id, "Pizza Co");
    }

    @Test
    void renamingAFranchiseToAnExistingNameIgnoringCaseAnswersWith409() {
        franchise("Burger Co");
        UUID other = franchise("Pizza Co");

        patch(FRANCHISE_PATH, "{\"name\":\"BURGER CO\"}", other)
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.code").isEqualTo("DUPLICATE_NAME");

        assertStoredName("franchise", other, "Pizza Co");
    }

    @Test
    void renamingAnUnknownFranchiseAnswersWith404() {
        patch(FRANCHISE_PATH, "{\"name\":\"Pizza Co\"}", UUID.randomUUID())
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.code").isEqualTo("FRANCHISE_NOT_FOUND");
    }

    @Test
    void renamingAFranchiseWithABlankOrNonTextNameAnswersWith400() {
        UUID id = franchise("Burger Co");

        patch(FRANCHISE_PATH, "{\"name\":\"   \"}", id).expectStatus().isBadRequest();
        patch(FRANCHISE_PATH, "{\"name\":123}", id).expectStatus().isBadRequest();
        patch(FRANCHISE_PATH, "{}", id).expectStatus().isBadRequest();

        assertStoredName("franchise", id, "Burger Co");
    }

    // ---- branch

    @Test
    void renamesABranchOfItsFranchise() {
        UUID franchiseId = franchise("Burger Co");
        UUID id = branch(franchiseId, "Centro");

        patch(BRANCH_PATH, "{\"name\":\"Sur\"}", franchiseId, id)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.franchiseId").isEqualTo(franchiseId.toString())
                .jsonPath("$.name").isEqualTo("Sur");

        assertStoredName("branch", id, "Sur");
    }

    @Test
    void renamingABranchToAnExistingNameOfTheSameFranchiseAnswersWith409() {
        UUID franchiseId = franchise("Burger Co");
        branch(franchiseId, "Centro");
        UUID other = branch(franchiseId, "Norte");

        patch(BRANCH_PATH, "{\"name\":\"centro\"}", franchiseId, other)
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.code").isEqualTo("DUPLICATE_NAME");
    }

    @Test
    void renamingABranchThroughAnotherFranchiseAnswersWith404AndChangesNothing() {
        UUID first = franchise("Burger Co");
        UUID second = franchise("Pizza Co");
        UUID id = branch(second, "Norte");

        patch(BRANCH_PATH, "{\"name\":\"Sur\"}", first, id)
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.code").isEqualTo("BRANCH_NOT_FOUND");

        assertStoredName("branch", id, "Norte");
    }

    // ---- product

    @Test
    void renamesAProductAndKeepsItsStock() {
        UUID branchId = branch(franchise("Burger Co"), "Centro");
        UUID id = product(branchId, "Hamburguesa");

        patch(PRODUCT_PATH, "{\"name\":\"Pizza\"}", branchId, id)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Pizza")
                .jsonPath("$.stock").isEqualTo(10);

        assertStoredName("product", id, "Pizza");
    }

    @Test
    void renamingAProductToAnExistingNameOfTheSameBranchAnswersWith409() {
        UUID branchId = branch(franchise("Burger Co"), "Centro");
        product(branchId, "Hamburguesa");
        UUID other = product(branchId, "Gaseosa");

        patch(PRODUCT_PATH, "{\"name\":\"HAMBURGUESA\"}", branchId, other)
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.code").isEqualTo("DUPLICATE_NAME");

        assertStoredName("product", other, "Gaseosa");
    }

    @Test
    void renamingAProductThroughAnotherBranchAnswersWith404AndChangesNothing() {
        UUID franchiseId = franchise("Burger Co");
        UUID first = branch(franchiseId, "Centro");
        UUID second = branch(franchiseId, "Norte");
        UUID id = product(second, "Hamburguesa");

        patch(PRODUCT_PATH, "{\"name\":\"Pizza\"}", first, id)
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.code").isEqualTo("PRODUCT_NOT_FOUND");

        assertStoredName("product", id, "Hamburguesa");
    }
}
