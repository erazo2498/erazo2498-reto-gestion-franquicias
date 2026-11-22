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
class UpdateProductStockApiDocumentationTest extends PostgresIntegrationTest {

    private static final String PATCH_STOCK = "$.paths['/api/v1/branches/{branchId}/products/{productId}/stock'].patch";
    private static final String JSON = "['application/json']";

    @Autowired
    private WebTestClient webTestClient;

    private WebTestClient.BodyContentSpec apiDocs() {
        return webTestClient.get().uri("/v3/api-docs").exchange().expectStatus().isOk().expectBody();
    }

    @Test
    void documentsTheTwoPathParameters() {
        apiDocs()
                .jsonPath(PATCH_STOCK + ".summary").isNotEmpty()
                .jsonPath(PATCH_STOCK + ".parameters[0].name").isEqualTo("branchId")
                .jsonPath(PATCH_STOCK + ".parameters[0].schema.format").isEqualTo("uuid")
                .jsonPath(PATCH_STOCK + ".parameters[1].name").isEqualTo("productId")
                .jsonPath(PATCH_STOCK + ".parameters[1].schema.format").isEqualTo("uuid");
    }

    @Test
    void documentsTheRequestWithAnExample() {
        apiDocs()
                .jsonPath(PATCH_STOCK + ".requestBody.required").isEqualTo(true)
                .jsonPath(PATCH_STOCK + ".requestBody.content" + JSON + ".examples.validStock.value.stock")
                .isEqualTo(25);
    }

    @Test
    void documentsTheUpdatedProductResponseWithAnExample() {
        apiDocs()
                .jsonPath(PATCH_STOCK + ".responses['200'].description").isNotEmpty()
                .jsonPath(PATCH_STOCK + ".responses['200'].content" + JSON + ".examples.updated.value.id")
                .isNotEmpty()
                .jsonPath(PATCH_STOCK + ".responses['200'].content" + JSON + ".examples.updated.value.stock")
                .isEqualTo(25);
    }

    @Test
    void documentsTheErrorResponsesWithExamples() {
        apiDocs()
                .jsonPath(PATCH_STOCK + ".responses['400'].content" + JSON + ".examples.invalidStock.value.code")
                .isEqualTo("INVALID_DATA")
                .jsonPath(PATCH_STOCK + ".responses['404'].content" + JSON + ".examples.productNotFound.value.code")
                .isEqualTo("PRODUCT_NOT_FOUND")
                .jsonPath(PATCH_STOCK + ".responses['415'].content" + JSON
                        + ".examples.unsupportedMediaType.value.code")
                .isEqualTo("UNSUPPORTED_MEDIA_TYPE")
                .jsonPath(PATCH_STOCK + ".responses['500'].content" + JSON + ".examples.unexpectedError.value.code")
                .isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void describesTheRequestSchema() {
        apiDocs()
                .jsonPath("$.components.schemas.UpdateProductStockRequest.properties.stock.type")
                .isEqualTo("integer")
                .jsonPath("$.components.schemas.UpdateProductStockRequest.properties.stock.minimum")
                .isEqualTo(0)
                .jsonPath("$.components.schemas.UpdateProductStockRequest.required[0]").isEqualTo("stock");
    }

    @Test
    void keepsTheOtherEndpointsDocumented() {
        apiDocs()
                .jsonPath("$.paths['/api/v1/branches/{branchId}/products'].post.operationId")
                .isEqualTo("createProduct")
                .jsonPath("$.paths['/api/v1/branches/{branchId}/products/{productId}'].delete.operationId")
                .isEqualTo("deleteProduct")
                .jsonPath("$.components.schemas.ProductResponse.required.length()").isEqualTo(4);
    }
}
