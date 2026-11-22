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
class RenameApiDocumentationTest extends PostgresIntegrationTest {

    private static final String PATCH_FRANCHISE = "$.paths['/api/v1/franchises/{franchiseId}'].patch";
    private static final String PATCH_BRANCH =
            "$.paths['/api/v1/franchises/{franchiseId}/branches/{branchId}'].patch";
    private static final String PATCH_PRODUCT = "$.paths['/api/v1/branches/{branchId}/products/{productId}'].patch";
    private static final String JSON = "['application/json']";

    @Autowired
    private WebTestClient webTestClient;

    private WebTestClient.BodyContentSpec apiDocs() {
        return webTestClient.get().uri("/v3/api-docs").exchange().expectStatus().isOk().expectBody();
    }

    @Test
    void documentsRenamingAFranchise() {
        apiDocs()
                .jsonPath(PATCH_FRANCHISE + ".operationId").isEqualTo("renameFranchise")
                .jsonPath(PATCH_FRANCHISE + ".parameters[0].name").isEqualTo("franchiseId")
                .jsonPath(PATCH_FRANCHISE + ".parameters[0].schema.format").isEqualTo("uuid")
                .jsonPath(PATCH_FRANCHISE + ".requestBody.content" + JSON + ".examples.newName.value.name")
                .isNotEmpty()
                .jsonPath(PATCH_FRANCHISE + ".responses['200'].content" + JSON + ".examples.renamed.value.id")
                .isNotEmpty()
                .jsonPath(PATCH_FRANCHISE + ".responses['404'].content" + JSON + ".examples.notFound.value.code")
                .isEqualTo("FRANCHISE_NOT_FOUND")
                .jsonPath(PATCH_FRANCHISE + ".responses['409'].content" + JSON + ".examples.duplicateName.value.code")
                .isEqualTo("DUPLICATE_NAME")
                .jsonPath(PATCH_FRANCHISE + ".responses['400'].content" + JSON + ".examples.invalidName.value.code")
                .isEqualTo("INVALID_DATA");
    }

    @Test
    void documentsRenamingABranch() {
        apiDocs()
                .jsonPath(PATCH_BRANCH + ".operationId").isEqualTo("renameBranch")
                .jsonPath(PATCH_BRANCH + ".parameters[0].name").isEqualTo("franchiseId")
                .jsonPath(PATCH_BRANCH + ".parameters[1].name").isEqualTo("branchId")
                .jsonPath(PATCH_BRANCH + ".responses['200'].content" + JSON + ".examples.renamed.value.franchiseId")
                .isNotEmpty()
                .jsonPath(PATCH_BRANCH + ".responses['404'].content" + JSON + ".examples.notFound.value.code")
                .isEqualTo("BRANCH_NOT_FOUND")
                .jsonPath(PATCH_BRANCH + ".responses['409'].content" + JSON + ".examples.duplicateName.value.code")
                .isEqualTo("DUPLICATE_NAME");
    }

    @Test
    void documentsRenamingAProduct() {
        apiDocs()
                .jsonPath(PATCH_PRODUCT + ".operationId").isEqualTo("renameProduct")
                .jsonPath(PATCH_PRODUCT + ".parameters[0].name").isEqualTo("branchId")
                .jsonPath(PATCH_PRODUCT + ".parameters[1].name").isEqualTo("productId")
                .jsonPath(PATCH_PRODUCT + ".responses['200'].content" + JSON + ".examples.renamed.value.stock")
                .isNotEmpty()
                .jsonPath(PATCH_PRODUCT + ".responses['404'].content" + JSON + ".examples.notFound.value.code")
                .isEqualTo("PRODUCT_NOT_FOUND")
                .jsonPath(PATCH_PRODUCT + ".responses['409'].content" + JSON + ".examples.duplicateName.value.code")
                .isEqualTo("DUPLICATE_NAME");
    }

    @Test
    void describesTheRenameRequestSchema() {
        apiDocs()
                .jsonPath("$.components.schemas.RenameRequest.properties.name.minLength").isEqualTo(1)
                .jsonPath("$.components.schemas.RenameRequest.properties.name.maxLength").isEqualTo(120)
                .jsonPath("$.components.schemas.RenameRequest.required[0]").isEqualTo("name");
    }
}
