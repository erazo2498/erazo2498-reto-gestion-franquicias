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
class CreateBranchEndToEndTest extends PostgresIntegrationTest {

    private static final String PATH = "/api/v1/franchises/{franchiseId}/branches";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    @AfterEach
    void cleanTables() {
        // Branches first: they reference franchises. Cleaning also after each test keeps other test classes unaffected.
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

    private WebTestClient.ResponseSpec post(Object franchiseId, String body) {
        return webTestClient
                .post()
                .uri(PATH, franchiseId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    @Test
    void createsABranchWithTheNameTrimmedAndStoresItForTheFranchise() {
        UUID franchiseId = existingFranchise("Burger Co");

        post(franchiseId, "{\"name\":\"  Centro  \"}")
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.franchiseId").isEqualTo(franchiseId.toString())
                .jsonPath("$.name").isEqualTo("Centro");

        StepVerifier.create(databaseClient.sql("SELECT name FROM branch WHERE franchise_id = :franchiseId")
                        .bind("franchiseId", franchiseId)
                        .map(row -> row.get("name", String.class))
                        .one())
                .assertNext(name -> assertEquals("Centro", name))
                .verifyComplete();
    }

    @Test
    void answersWith404WhenTheFranchiseDoesNotExist() {
        post(UUID.randomUUID(), "{\"name\":\"Centro\"}")
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("FRANCHISE_NOT_FOUND");
    }

    @Test
    void answersWith409WhenTheNameExistsInTheSameFranchiseIgnoringCase() {
        UUID franchiseId = existingFranchise("Burger Co");
        post(franchiseId, "{\"name\":\"Centro\"}").expectStatus().isCreated();

        post(franchiseId, "{\"name\":\" CENTRO \"}")
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("DUPLICATE_NAME");
    }

    @Test
    void allowsTheSameNameInAnotherFranchise() {
        UUID firstFranchise = existingFranchise("Burger Co");
        UUID secondFranchise = existingFranchise("Pizza Co");
        post(firstFranchise, "{\"name\":\"Centro\"}").expectStatus().isCreated();

        post(secondFranchise, "{\"name\":\"Centro\"}").expectStatus().isCreated();
    }

    @Test
    void answersWith400WhenTheNameIsBlank() {
        UUID franchiseId = existingFranchise("Burger Co");

        post(franchiseId, "{\"name\":\"   \"}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");
    }

    @Test
    void answersWith400WhenTheFranchiseIdIsNotAUuid() {
        post("abc", "{\"name\":\"Centro\"}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");
    }
}
