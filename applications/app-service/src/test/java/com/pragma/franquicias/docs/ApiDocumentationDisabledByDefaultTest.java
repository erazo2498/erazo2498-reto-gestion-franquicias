package com.pragma.franquicias.docs;

import com.pragma.franquicias.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ApiDocumentationDisabledByDefaultTest extends PostgresIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void doesNotPublishTheApiDocsWithoutAnActiveProfile() {
        webTestClient.get().uri("/v3/api-docs").exchange().expectStatus().isNotFound();
    }

    @Test
    void doesNotPublishSwaggerUiWithoutAnActiveProfile() {
        webTestClient.get().uri("/swagger-ui.html").exchange().expectStatus().isNotFound();
    }
}
