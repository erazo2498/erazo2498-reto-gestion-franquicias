package com.pragma.franquicias.api.rename;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pragma.franquicias.api.config.JacksonConfig;
import com.pragma.franquicias.api.error.ApiErrorHandler;
import com.pragma.franquicias.model.branch.Branch;
import com.pragma.franquicias.model.branch.exceptions.BranchNotFoundException;
import com.pragma.franquicias.model.branch.exceptions.DuplicateBranchNameException;
import com.pragma.franquicias.model.franchise.Franchise;
import com.pragma.franquicias.model.franchise.exceptions.DuplicateFranchiseNameException;
import com.pragma.franquicias.model.franchise.exceptions.FranchiseNotFoundException;
import com.pragma.franquicias.model.franchise.exceptions.InvalidFranchiseNameException;
import com.pragma.franquicias.model.product.Product;
import com.pragma.franquicias.model.product.exceptions.DuplicateProductNameException;
import com.pragma.franquicias.model.product.exceptions.ProductNotFoundException;
import com.pragma.franquicias.usecase.renamebranch.RenameBranchUseCase;
import com.pragma.franquicias.usecase.renamefranchise.RenameFranchiseUseCase;
import com.pragma.franquicias.usecase.renameproduct.RenameProductUseCase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest
@ContextConfiguration(
        classes = {RenameRouter.class, RenameHandler.class, ApiErrorHandler.class, JacksonConfig.class})
class RenameRouteTest {

    private static final UUID FRANCHISE_ID = UUID.randomUUID();
    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final String FRANCHISE_PATH = "/api/v1/franchises/{franchiseId}";
    private static final String BRANCH_PATH = "/api/v1/franchises/{franchiseId}/branches/{branchId}";
    private static final String PRODUCT_PATH = "/api/v1/branches/{branchId}/products/{productId}";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private RenameFranchiseUseCase renameFranchiseUseCase;

    @MockitoBean
    private RenameBranchUseCase renameBranchUseCase;

    @MockitoBean
    private RenameProductUseCase renameProductUseCase;

    private WebTestClient.RequestHeadersSpec<?> patch(String path, String body, Object... ids) {
        return webTestClient.patch().uri(path, ids).contentType(MediaType.APPLICATION_JSON).bodyValue(body);
    }

    // ---- franchise

    @Test
    void renamesAFranchiseAndAnswersWith200() {
        when(renameFranchiseUseCase.execute(FRANCHISE_ID, "Pizza Co"))
                .thenReturn(Mono.just(Franchise.builder().id(FRANCHISE_ID).name("Pizza Co").build()));

        patch(FRANCHISE_PATH, "{\"name\":\"Pizza Co\"}", FRANCHISE_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(FRANCHISE_ID.toString())
                .jsonPath("$.name").isEqualTo("Pizza Co");
    }

    @Test
    void renamingAFranchiseAnswersWith404409And400() {
        when(renameFranchiseUseCase.execute(FRANCHISE_ID, "A"))
                .thenReturn(Mono.error(new FranchiseNotFoundException(FRANCHISE_ID)));
        when(renameFranchiseUseCase.execute(FRANCHISE_ID, "B"))
                .thenReturn(Mono.error(new DuplicateFranchiseNameException("B")));
        when(renameFranchiseUseCase.execute(FRANCHISE_ID, ""))
                .thenReturn(Mono.error(new InvalidFranchiseNameException()));

        patch(FRANCHISE_PATH, "{\"name\":\"A\"}", FRANCHISE_ID)
                .exchange().expectStatus().isNotFound()
                .expectBody().jsonPath("$.code").isEqualTo("FRANCHISE_NOT_FOUND");
        patch(FRANCHISE_PATH, "{\"name\":\"B\"}", FRANCHISE_ID)
                .exchange().expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.code").isEqualTo("DUPLICATE_NAME");
        patch(FRANCHISE_PATH, "{\"name\":\"\"}", FRANCHISE_ID)
                .exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_DATA");
    }

    @Test
    void renamingAFranchiseRejectsABadIdAndANameThatIsNotText() {
        patch(FRANCHISE_PATH, "{\"name\":\"Pizza Co\"}", "abc")
                .exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").isEqualTo("Franchise id must be a valid UUID");
        patch(FRANCHISE_PATH, "{\"name\":123}", FRANCHISE_ID)
                .exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("INVALID_DATA");

        verifyNoInteractions(renameFranchiseUseCase);
    }

    // ---- branch

    @Test
    void renamesABranchAndAnswersWith200() {
        when(renameBranchUseCase.execute(FRANCHISE_ID, BRANCH_ID, "Sur"))
                .thenReturn(Mono.just(
                        Branch.builder().id(BRANCH_ID).franchiseId(FRANCHISE_ID).name("Sur").build()));

        patch(BRANCH_PATH, "{\"name\":\"Sur\"}", FRANCHISE_ID, BRANCH_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(BRANCH_ID.toString())
                .jsonPath("$.franchiseId").isEqualTo(FRANCHISE_ID.toString())
                .jsonPath("$.name").isEqualTo("Sur");
    }

    @Test
    void renamingABranchAnswersWith404And409() {
        when(renameBranchUseCase.execute(FRANCHISE_ID, BRANCH_ID, "A"))
                .thenReturn(Mono.error(new BranchNotFoundException(BRANCH_ID)));
        when(renameBranchUseCase.execute(FRANCHISE_ID, BRANCH_ID, "B"))
                .thenReturn(Mono.error(new DuplicateBranchNameException("B")));

        patch(BRANCH_PATH, "{\"name\":\"A\"}", FRANCHISE_ID, BRANCH_ID)
                .exchange().expectStatus().isNotFound()
                .expectBody().jsonPath("$.code").isEqualTo("BRANCH_NOT_FOUND");
        patch(BRANCH_PATH, "{\"name\":\"B\"}", FRANCHISE_ID, BRANCH_ID)
                .exchange().expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.code").isEqualTo("DUPLICATE_NAME");
    }

    @Test
    void renamingABranchRejectsBadIds() {
        patch(BRANCH_PATH, "{\"name\":\"Sur\"}", "abc", BRANCH_ID)
                .exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").isEqualTo("Franchise id must be a valid UUID");
        patch(BRANCH_PATH, "{\"name\":\"Sur\"}", FRANCHISE_ID, "abc")
                .exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").isEqualTo("Branch id must be a valid UUID");

        verifyNoInteractions(renameBranchUseCase);
    }

    // ---- product

    @Test
    void renamesAProductAndAnswersWith200() {
        when(renameProductUseCase.execute(BRANCH_ID, PRODUCT_ID, "Pizza"))
                .thenReturn(Mono.just(
                        Product.builder().id(PRODUCT_ID).branchId(BRANCH_ID).name("Pizza").stock(3).build()));

        patch(PRODUCT_PATH, "{\"name\":\"Pizza\"}", BRANCH_ID, PRODUCT_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(PRODUCT_ID.toString())
                .jsonPath("$.name").isEqualTo("Pizza")
                .jsonPath("$.stock").isEqualTo(3);
    }

    @Test
    void renamingAProductAnswersWith404And409() {
        when(renameProductUseCase.execute(BRANCH_ID, PRODUCT_ID, "A"))
                .thenReturn(Mono.error(new ProductNotFoundException(BRANCH_ID, PRODUCT_ID)));
        when(renameProductUseCase.execute(BRANCH_ID, PRODUCT_ID, "B"))
                .thenReturn(Mono.error(new DuplicateProductNameException("B")));

        patch(PRODUCT_PATH, "{\"name\":\"A\"}", BRANCH_ID, PRODUCT_ID)
                .exchange().expectStatus().isNotFound()
                .expectBody().jsonPath("$.code").isEqualTo("PRODUCT_NOT_FOUND");
        patch(PRODUCT_PATH, "{\"name\":\"B\"}", BRANCH_ID, PRODUCT_ID)
                .exchange().expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.code").isEqualTo("DUPLICATE_NAME");
    }

    @Test
    void renamingAProductRejectsBadIds() {
        patch(PRODUCT_PATH, "{\"name\":\"Pizza\"}", "abc", PRODUCT_ID)
                .exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").isEqualTo("Branch id must be a valid UUID");
        patch(PRODUCT_PATH, "{\"name\":\"Pizza\"}", BRANCH_ID, "abc")
                .exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").isEqualTo("Product id must be a valid UUID");

        verifyNoInteractions(renameProductUseCase);
    }

    @Test
    void answersWith415WhenTheContentTypeIsNotJson() {
        webTestClient.patch()
                .uri(PRODUCT_PATH, BRANCH_ID, PRODUCT_ID)
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue("Pizza")
                .exchange()
                .expectStatus().isEqualTo(415)
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    void answersWith500WithoutLeakingInternalDetailsOnUnexpectedErrors() {
        when(renameProductUseCase.execute(BRANCH_ID, PRODUCT_ID, "Pizza"))
                .thenReturn(Mono.error(new IllegalStateException("connection refused to 10.0.0.5")));

        patch(PRODUCT_PATH, "{\"name\":\"Pizza\"}", BRANCH_ID, PRODUCT_ID)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INTERNAL_ERROR")
                .jsonPath("$.message").isEqualTo("Unexpected error");
    }
}
