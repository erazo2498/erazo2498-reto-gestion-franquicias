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
class BranchApiDocumentationTest extends PostgresIntegrationTest {

    private static final String POST_BRANCHES = "$.paths['/api/v1/franchises/{franchiseId}/branches'].post";
    private static final String JSON = "['application/json']";

    @Autowired
    private WebTestClient webTestClient;

    private WebTestClient.BodyContentSpec apiDocs() {
        return webTestClient.get().uri("/v3/api-docs").exchange().expectStatus().isOk().expectBody();
    }

    @Test
    void documentsTheFranchiseIdPathParameter() {
        apiDocs()
                .jsonPath(POST_BRANCHES + ".summary").isNotEmpty()
                .jsonPath(POST_BRANCHES + ".parameters[0].name").isEqualTo("franchiseId")
                .jsonPath(POST_BRANCHES + ".parameters[0].in").isEqualTo("path")
                .jsonPath(POST_BRANCHES + ".parameters[0].required").isEqualTo(true)
                .jsonPath(POST_BRANCHES + ".parameters[0].schema.format").isEqualTo("uuid");
    }

    @Test
    void documentsTheCreateBranchRequestWithAnExample() {
        apiDocs()
                .jsonPath(POST_BRANCHES + ".requestBody.required").isEqualTo(true)
                .jsonPath(POST_BRANCHES + ".requestBody.content" + JSON + ".examples.validName.value.name")
                .isEqualTo("Centro");
    }

    @Test
    void documentsTheCreatedResponseWithAnExample() {
        apiDocs()
                .jsonPath(POST_BRANCHES + ".responses['201'].description").isNotEmpty()
                .jsonPath(POST_BRANCHES + ".responses['201'].content" + JSON + ".examples.created.value.id")
                .isNotEmpty()
                .jsonPath(POST_BRANCHES + ".responses['201'].content" + JSON + ".examples.created.value.franchiseId")
                .isNotEmpty()
                .jsonPath(POST_BRANCHES + ".responses['201'].content" + JSON + ".examples.created.value.name")
                .isEqualTo("Centro");
    }

    @Test
    void documentsTheErrorResponsesWithExamples() {
        apiDocs()
                .jsonPath(POST_BRANCHES + ".responses['400'].content" + JSON + ".examples.invalidName.value.code")
                .isEqualTo("INVALID_DATA")
                .jsonPath(POST_BRANCHES + ".responses['404'].content" + JSON
                        + ".examples.franchiseNotFound.value.code")
                .isEqualTo("FRANCHISE_NOT_FOUND")
                .jsonPath(POST_BRANCHES + ".responses['409'].content" + JSON + ".examples.duplicateName.value.code")
                .isEqualTo("DUPLICATE_NAME")
                .jsonPath(POST_BRANCHES + ".responses['415'].content" + JSON
                        + ".examples.unsupportedMediaType.value.code")
                .isEqualTo("UNSUPPORTED_MEDIA_TYPE")
                .jsonPath(POST_BRANCHES + ".responses['500'].content" + JSON + ".examples.unexpectedError.value.code")
                .isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void describesTheSchemasOfTheBranch() {
        apiDocs()
                .jsonPath("$.components.schemas.CreateBranchRequest.properties.name.minLength").isEqualTo(1)
                .jsonPath("$.components.schemas.CreateBranchRequest.properties.name.maxLength").isEqualTo(120)
                .jsonPath("$.components.schemas.CreateBranchRequest.required[0]").isEqualTo("name")
                .jsonPath("$.components.schemas.BranchResponse.required.length()").isEqualTo(3)
                .jsonPath("$.components.schemas.BranchResponse.properties.franchiseId.format").isEqualTo("uuid");
    }

    @Test
    void keepsASingleSharedErrorSchemaAndTheFranchiseEndpoint() {
        apiDocs()
                .jsonPath("$.components.schemas.ErrorResponse.required.length()").isEqualTo(3)
                .jsonPath("$.paths['/api/v1/franchises'].post.operationId").isEqualTo("createFranchise");
    }
}
