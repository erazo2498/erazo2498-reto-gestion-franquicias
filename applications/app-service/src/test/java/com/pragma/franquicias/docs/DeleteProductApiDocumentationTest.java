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
class DeleteProductApiDocumentationTest extends PostgresIntegrationTest {

    private static final String DELETE_PRODUCT =
            "$.paths['/api/v1/branches/{branchId}/products/{productId}'].delete";
    private static final String JSON = "['application/json']";

    @Autowired
    private WebTestClient webTestClient;

    private WebTestClient.BodyContentSpec apiDocs() {
        return webTestClient.get().uri("/v3/api-docs").exchange().expectStatus().isOk().expectBody();
    }

    @Test
    void documentsTheTwoPathParameters() {
        apiDocs()
                .jsonPath(DELETE_PRODUCT + ".summary").isNotEmpty()
                .jsonPath(DELETE_PRODUCT + ".parameters[0].name").isEqualTo("branchId")
                .jsonPath(DELETE_PRODUCT + ".parameters[0].in").isEqualTo("path")
                .jsonPath(DELETE_PRODUCT + ".parameters[0].required").isEqualTo(true)
                .jsonPath(DELETE_PRODUCT + ".parameters[0].schema.format").isEqualTo("uuid")
                .jsonPath(DELETE_PRODUCT + ".parameters[1].name").isEqualTo("productId")
                .jsonPath(DELETE_PRODUCT + ".parameters[1].in").isEqualTo("path")
                .jsonPath(DELETE_PRODUCT + ".parameters[1].required").isEqualTo(true)
                .jsonPath(DELETE_PRODUCT + ".parameters[1].schema.format").isEqualTo("uuid");
    }

    @Test
    void documentsThe204AnswerWithoutABodyAsIdempotent() {
        apiDocs()
                .jsonPath(DELETE_PRODUCT + ".responses['204'].description").isNotEmpty()
                .jsonPath(DELETE_PRODUCT + ".responses['204'].content").doesNotExist();
    }

    @Test
    void documentsTheErrorResponsesWithExamples() {
        apiDocs()
                .jsonPath(DELETE_PRODUCT + ".responses['400'].content" + JSON + ".examples.invalidId.value.code")
                .isEqualTo("INVALID_DATA")
                .jsonPath(DELETE_PRODUCT + ".responses['400'].content" + JSON + ".examples.invalidId.value.message")
                .isEqualTo("Product id must be a valid UUID")
                .jsonPath(DELETE_PRODUCT + ".responses['500'].content" + JSON
                        + ".examples.unexpectedError.value.code")
                .isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void doesNotDocumentA404BecauseDeletingIsIdempotent() {
        apiDocs().jsonPath(DELETE_PRODUCT + ".responses['404']").doesNotExist();
    }

    @Test
    void keepsTheOtherEndpointsDocumented() {
        apiDocs()
                .jsonPath("$.paths['/api/v1/branches/{branchId}/products'].post.operationId")
                .isEqualTo("createProduct")
                .jsonPath("$.paths['/api/v1/franchises/{franchiseId}/branches'].post.operationId")
                .isEqualTo("createBranch")
                .jsonPath("$.paths['/api/v1/franchises'].post.operationId").isEqualTo("createFranchise")
                .jsonPath("$.components.schemas.ErrorResponse.required.length()").isEqualTo(3);
    }
}
