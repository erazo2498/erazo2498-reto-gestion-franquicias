package com.pragma.franquicias.docs;

import com.pragma.franquicias.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("dev")
class ApiDocumentationTest extends PostgresIntegrationTest {

    private static final String POST_FRANCHISES = "$.paths['/api/v1/franchises'].post";
    private static final String JSON = "['application/json']";

    @Autowired
    private WebTestClient webTestClient;

    private WebTestClient.BodyContentSpec apiDocs() {
        return webTestClient.get().uri("/v3/api-docs").exchange().expectStatus().isOk().expectBody();
    }

    @Test
    void describesTheApi() {
        apiDocs()
                .jsonPath("$.info.title").isEqualTo("Franchise Management API")
                .jsonPath("$.info.version").isNotEmpty();
    }

    @Test
    void documentsTheCreateFranchiseRequestWithAnExample() {
        apiDocs()
                .jsonPath(POST_FRANCHISES + ".summary").isNotEmpty()
                .jsonPath(POST_FRANCHISES + ".requestBody.required").isEqualTo(true)
                .jsonPath(POST_FRANCHISES + ".requestBody.content" + JSON + ".examples.validName.value.name")
                .isEqualTo("Burger Co");
    }

    @Test
    void documentsTheCreatedResponseWithAnExample() {
        apiDocs()
                .jsonPath(POST_FRANCHISES + ".responses['201'].description").isNotEmpty()
                .jsonPath(POST_FRANCHISES + ".responses['201'].content" + JSON + ".examples.created.value.id")
                .isNotEmpty()
                .jsonPath(POST_FRANCHISES + ".responses['201'].content" + JSON + ".examples.created.value.name")
                .isEqualTo("Burger Co");
    }

    @Test
    void documentsTheErrorResponsesWithExamples() {
        apiDocs()
                .jsonPath(POST_FRANCHISES + ".responses['400'].content" + JSON + ".examples.invalidName.value.code")
                .isEqualTo("INVALID_DATA")
                .jsonPath(POST_FRANCHISES + ".responses['409'].content" + JSON + ".examples.duplicateName.value.code")
                .isEqualTo("DUPLICATE_NAME")
                .jsonPath(POST_FRANCHISES + ".responses['415'].content" + JSON
                        + ".examples.unsupportedMediaType.value.code")
                .isEqualTo("UNSUPPORTED_MEDIA_TYPE")
                .jsonPath(POST_FRANCHISES + ".responses['500'].content" + JSON + ".examples.unexpectedError.value.code")
                .isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void describesTheNameConstraintsInTheRequestSchema() {
        apiDocs()
                .jsonPath("$.components.schemas.CreateFranchiseRequest.properties.name.minLength").isEqualTo(1)
                .jsonPath("$.components.schemas.CreateFranchiseRequest.properties.name.maxLength").isEqualTo(120)
                .jsonPath("$.components.schemas.CreateFranchiseRequest.required[0]").isEqualTo("name");
    }

    @Test
    void servesSwaggerUi() {
        webTestClient.get().uri("/swagger-ui.html").exchange().expectStatus().is3xxRedirection();
    }
}
