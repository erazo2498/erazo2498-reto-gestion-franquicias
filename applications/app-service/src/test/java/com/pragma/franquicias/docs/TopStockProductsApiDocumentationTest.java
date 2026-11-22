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
class TopStockProductsApiDocumentationTest extends PostgresIntegrationTest {

    private static final String GET_TOP = "$.paths['/api/v1/franchises/{franchiseId}/top-stock-products'].get";
    private static final String JSON = "['application/json']";

    @Autowired
    private WebTestClient webTestClient;

    private WebTestClient.BodyContentSpec apiDocs() {
        return webTestClient.get().uri("/v3/api-docs").exchange().expectStatus().isOk().expectBody();
    }

    @Test
    void documentsTheFranchiseIdPathParameter() {
        apiDocs()
                .jsonPath(GET_TOP + ".summary").isNotEmpty()
                .jsonPath(GET_TOP + ".parameters[0].name").isEqualTo("franchiseId")
                .jsonPath(GET_TOP + ".parameters[0].in").isEqualTo("path")
                .jsonPath(GET_TOP + ".parameters[0].schema.format").isEqualTo("uuid");
    }

    @Test
    void documentsTheListResponseWithAnExampleIncludingAnEmptyBranch() {
        apiDocs()
                .jsonPath(GET_TOP + ".responses['200'].content" + JSON + ".schema.type").isEqualTo("array")
                .jsonPath(GET_TOP + ".responses['200'].content" + JSON + ".examples.topProducts.value[0].branchName")
                .isEqualTo("Centro")
                .jsonPath(GET_TOP + ".responses['200'].content" + JSON + ".examples.topProducts.value[0].product.stock")
                .isEqualTo(40)
                .jsonPath(GET_TOP + ".responses['200'].content" + JSON + ".examples.topProducts.value[1].product")
                .isEmpty();
    }

    @Test
    void documentsTheErrorResponsesWithExamples() {
        apiDocs()
                .jsonPath(GET_TOP + ".responses['400'].content" + JSON + ".examples.invalidId.value.code")
                .isEqualTo("INVALID_DATA")
                .jsonPath(GET_TOP + ".responses['404'].content" + JSON + ".examples.franchiseNotFound.value.code")
                .isEqualTo("FRANCHISE_NOT_FOUND")
                .jsonPath(GET_TOP + ".responses['500'].content" + JSON + ".examples.unexpectedError.value.code")
                .isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void describesTheItemSchema() {
        apiDocs()
                .jsonPath("$.components.schemas.TopStockProductResponse.required.length()").isEqualTo(2)
                .jsonPath("$.components.schemas.TopStockProductResponse.properties.product.nullable")
                .isEqualTo(true)
                .jsonPath("$.components.schemas.TopStockProductResponse.properties.branchId.format")
                .isEqualTo("uuid");
    }
}
