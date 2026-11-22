package com.pragma.franquicias.api;

import com.pragma.franquicias.support.PostgresIntegrationTest;
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
class CreateFranchiseEndToEndTest extends PostgresIntegrationTest {

    private static final String PATH = "/api/v1/franchises";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    void cleanTable() {
        StepVerifier.create(databaseClient.sql("DELETE FROM branch").then()).verifyComplete();
        StepVerifier.create(databaseClient.sql("DELETE FROM franchise").then()).verifyComplete();
    }

    private WebTestClient.ResponseSpec post(String body) {
        return webTestClient.post().uri(PATH).contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
    }

    @Test
    void createsAFranchiseWithTheNameTrimmedAndAGeneratedId() {
        post("{\"name\":\"  Burger Co  \"}")
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.name").isEqualTo("Burger Co");
    }

    @Test
    void answersWith409WhenTheNameExistsIgnoringCase() {
        post("{\"name\":\"Burger Co\"}").expectStatus().isCreated();

        post("{\"name\":\"BURGER CO\"}")
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("DUPLICATE_NAME");
    }

    @Test
    void answersWith400WhenTheNameIsBlank() {
        post("{\"name\":\"   \"}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");
    }

    @Test
    void exposesOnlyTheHealthEndpointOfTheActuator() {
        webTestClient.get().uri("/actuator/health").exchange().expectStatus().isOk();
        webTestClient.get().uri("/actuator/prometheus").exchange().expectStatus().isNotFound();
        webTestClient.get().uri("/actuator/env").exchange().expectStatus().isNotFound();
    }
}
