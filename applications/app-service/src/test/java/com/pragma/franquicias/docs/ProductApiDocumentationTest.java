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
class ProductApiDocumentationTest extends PostgresIntegrationTest {

    private static final String POST_PRODUCTS = "$.paths['/api/v1/branches/{branchId}/products'].post";
    private static final String JSON = "['application/json']";

    @Autowired
    private WebTestClient webTestClient;

    private WebTestClient.BodyContentSpec apiDocs() {
        return webTestClient.get().uri("/v3/api-docs").exchange().expectStatus().isOk().expectBody();
    }

    @Test
    void documentsTheBranchIdPathParameter() {
        apiDocs()
                .jsonPath(POST_PRODUCTS + ".summary").isNotEmpty()
                .jsonPath(POST_PRODUCTS + ".parameters[0].name").isEqualTo("branchId")
                .jsonPath(POST_PRODUCTS + ".parameters[0].in").isEqualTo("path")
                .jsonPath(POST_PRODUCTS + ".parameters[0].required").isEqualTo(true)
                .jsonPath(POST_PRODUCTS + ".parameters[0].schema.format").isEqualTo("uuid");
    }

    @Test
    void documentsTheCreateProductRequestWithAnExample() {
        apiDocs()
                .jsonPath(POST_PRODUCTS + ".requestBody.required").isEqualTo(true)
                .jsonPath(POST_PRODUCTS + ".requestBody.content" + JSON + ".examples.validProduct.value.name")
                .isEqualTo("Hamburguesa")
                .jsonPath(POST_PRODUCTS + ".requestBody.content" + JSON + ".examples.validProduct.value.stock")
                .isEqualTo(10);
    }

    @Test
    void documentsTheCreatedResponseWithAnExample() {
        apiDocs()
                .jsonPath(POST_PRODUCTS + ".responses['201'].description").isNotEmpty()
                .jsonPath(POST_PRODUCTS + ".responses['201'].content" + JSON + ".examples.created.value.id")
                .isNotEmpty()
                .jsonPath(POST_PRODUCTS + ".responses['201'].content" + JSON + ".examples.created.value.branchId")
                .isNotEmpty()
                .jsonPath(POST_PRODUCTS + ".responses['201'].content" + JSON + ".examples.created.value.name")
                .isEqualTo("Hamburguesa")
                .jsonPath(POST_PRODUCTS + ".responses['201'].content" + JSON + ".examples.created.value.stock")
                .isEqualTo(10);
    }

    @Test
    void documentsTheErrorResponsesWithExamples() {
        apiDocs()
                .jsonPath(POST_PRODUCTS + ".responses['400'].content" + JSON + ".examples.invalidStock.value.code")
                .isEqualTo("INVALID_DATA")
                .jsonPath(POST_PRODUCTS + ".responses['404'].content" + JSON + ".examples.branchNotFound.value.code")
                .isEqualTo("BRANCH_NOT_FOUND")
                .jsonPath(POST_PRODUCTS + ".responses['409'].content" + JSON + ".examples.duplicateName.value.code")
                .isEqualTo("DUPLICATE_NAME")
                .jsonPath(POST_PRODUCTS + ".responses['415'].content" + JSON
                        + ".examples.unsupportedMediaType.value.code")
                .isEqualTo("UNSUPPORTED_MEDIA_TYPE")
                .jsonPath(POST_PRODUCTS + ".responses['500'].content" + JSON + ".examples.unexpectedError.value.code")
                .isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void describesTheSchemasOfTheProduct() {
        apiDocs()
                .jsonPath("$.components.schemas.CreateProductRequest.properties.name.minLength").isEqualTo(1)
                .jsonPath("$.components.schemas.CreateProductRequest.properties.name.maxLength").isEqualTo(120)
                .jsonPath("$.components.schemas.CreateProductRequest.properties.stock.type").isEqualTo("integer")
                .jsonPath("$.components.schemas.CreateProductRequest.properties.stock.minimum").isEqualTo(0)
                .jsonPath("$.components.schemas.CreateProductRequest.required.length()").isEqualTo(2)
                .jsonPath("$.components.schemas.ProductResponse.required.length()").isEqualTo(4)
                .jsonPath("$.components.schemas.ProductResponse.properties.branchId.format").isEqualTo("uuid")
                .jsonPath("$.components.schemas.ProductResponse.properties.stock.type").isEqualTo("integer");
    }

    @Test
    void keepsTheOtherEndpointsDocumented() {
        apiDocs()
                .jsonPath("$.paths['/api/v1/franchises'].post.operationId").isEqualTo("createFranchise")
                .jsonPath("$.paths['/api/v1/franchises/{franchiseId}/branches'].post.operationId")
                .isEqualTo("createBranch")
                .jsonPath("$.components.schemas.ErrorResponse.required.length()").isEqualTo(3);
    }
}
