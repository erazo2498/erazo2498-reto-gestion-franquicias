package com.pragma.franquicias.api.product;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pragma.franquicias.api.config.JacksonConfig;
import com.pragma.franquicias.api.error.ApiErrorHandler;
import com.pragma.franquicias.model.product.Product;
import com.pragma.franquicias.model.product.exceptions.InvalidProductStockException;
import com.pragma.franquicias.model.product.exceptions.ProductNotFoundException;
import com.pragma.franquicias.usecase.createproduct.CreateProductUseCase;
import com.pragma.franquicias.usecase.deleteproduct.DeleteProductUseCase;
import com.pragma.franquicias.usecase.updateproductstock.UpdateProductStockUseCase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest
@ContextConfiguration(
        classes = {ProductRouter.class, ProductHandler.class, ApiErrorHandler.class, JacksonConfig.class})
class UpdateProductStockRouteTest {

    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final String PATH = "/api/v1/branches/{branchId}/products/{productId}/stock";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UpdateProductStockUseCase updateProductStockUseCase;

    // The handler also needs the other use cases to be built.
    @MockitoBean
    private CreateProductUseCase createProductUseCase;

    @MockitoBean
    private DeleteProductUseCase deleteProductUseCase;

    private WebTestClient.RequestHeadersSpec<?> patchJson(Object branchId, Object productId, String body) {
        return webTestClient
                .patch()
                .uri(PATH, branchId, productId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }

    @Test
    void updatesTheStockAndAnswersWith200AndTheProduct() {
        when(updateProductStockUseCase.execute(BRANCH_ID, PRODUCT_ID, 25))
                .thenReturn(Mono.just(
                        Product.builder().id(PRODUCT_ID).branchId(BRANCH_ID).name("Hamburguesa").stock(25).build()));

        patchJson(BRANCH_ID, PRODUCT_ID, "{\"stock\":25}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(PRODUCT_ID.toString())
                .jsonPath("$.branchId").isEqualTo(BRANCH_ID.toString())
                .jsonPath("$.name").isEqualTo("Hamburguesa")
                .jsonPath("$.stock").isEqualTo(25);
    }

    @Test
    void answersWith400WhenTheStockIsMissing() {
        when(updateProductStockUseCase.execute(BRANCH_ID, PRODUCT_ID, null))
                .thenReturn(Mono.error(new InvalidProductStockException()));

        patchJson(BRANCH_ID, PRODUCT_ID, "{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA")
                .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    void answersWith400WhenTheStockIsNegative() {
        when(updateProductStockUseCase.execute(BRANCH_ID, PRODUCT_ID, -1))
                .thenReturn(Mono.error(new InvalidProductStockException()));

        patchJson(BRANCH_ID, PRODUCT_ID, "{\"stock\":-1}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");
    }

    @Test
    void answersWith400WhenTheBodyIsMissing() {
        when(updateProductStockUseCase.execute(BRANCH_ID, PRODUCT_ID, null))
                .thenReturn(Mono.error(new InvalidProductStockException()));

        webTestClient.patch()
                .uri(PATH, BRANCH_ID, PRODUCT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");
    }

    @Test
    void answersWith400WhenTheJsonIsMalformed() {
        patchJson(BRANCH_ID, PRODUCT_ID, "{\"stock\": ")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");

        verifyNoInteractions(updateProductStockUseCase);
    }

    // None of these may reach the use case: a decimal must not be cut to a whole number, a text must not be converted
    // to one, and a number beyond the range of an integer cannot be stored.
    @ParameterizedTest
    @ValueSource(strings = {"1.7", "\"5\"", "\"abc\"", "3000000000", "true"})
    void answersWith400WhenTheStockIsNotAWholeNumber(String stock) {
        patchJson(BRANCH_ID, PRODUCT_ID, "{\"stock\":" + stock + "}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");

        verifyNoInteractions(updateProductStockUseCase);
    }

    @Test
    void answersWith400WhenTheBranchIdIsNotAUuid() {
        patchJson("abc", PRODUCT_ID, "{\"stock\":25}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA")
                .jsonPath("$.message").isEqualTo("Branch id must be a valid UUID");

        verifyNoInteractions(updateProductStockUseCase);
    }

    @Test
    void answersWith400WhenTheProductIdIsNotAUuid() {
        patchJson(BRANCH_ID, "abc", "{\"stock\":25}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA")
                .jsonPath("$.message").isEqualTo("Product id must be a valid UUID");

        verifyNoInteractions(updateProductStockUseCase);
    }

    @Test
    void answersWith404WhenTheProductIsNotFoundInTheBranch() {
        when(updateProductStockUseCase.execute(BRANCH_ID, PRODUCT_ID, 25))
                .thenReturn(Mono.error(new ProductNotFoundException(BRANCH_ID, PRODUCT_ID)));

        patchJson(BRANCH_ID, PRODUCT_ID, "{\"stock\":25}")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("PRODUCT_NOT_FOUND")
                .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    void answersWith415WhenTheContentTypeIsNotJson() {
        webTestClient.patch()
                .uri(PATH, BRANCH_ID, PRODUCT_ID)
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue("25")
                .exchange()
                .expectStatus().isEqualTo(415)
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNSUPPORTED_MEDIA_TYPE");

        verifyNoInteractions(updateProductStockUseCase);
    }

    @Test
    void answersWith500WithoutLeakingInternalDetailsOnUnexpectedErrors() {
        when(updateProductStockUseCase.execute(BRANCH_ID, PRODUCT_ID, 25))
                .thenReturn(Mono.error(new IllegalStateException("connection refused to 10.0.0.5")));

        patchJson(BRANCH_ID, PRODUCT_ID, "{\"stock\":25}")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INTERNAL_ERROR")
                .jsonPath("$.message").isEqualTo("Unexpected error")
                .jsonPath("$.timestamp").isNotEmpty();
    }
}
