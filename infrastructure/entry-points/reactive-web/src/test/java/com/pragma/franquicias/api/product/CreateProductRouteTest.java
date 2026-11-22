package com.pragma.franquicias.api.product;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pragma.franquicias.api.config.JacksonConfig;
import com.pragma.franquicias.api.error.ApiErrorHandler;
import com.pragma.franquicias.model.branch.exceptions.BranchNotFoundException;
import com.pragma.franquicias.model.product.Product;
import com.pragma.franquicias.model.product.exceptions.DuplicateProductNameException;
import com.pragma.franquicias.model.product.exceptions.InvalidProductNameException;
import com.pragma.franquicias.model.product.exceptions.InvalidProductStockException;
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
class CreateProductRouteTest {

    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final String PATH = "/api/v1/branches/{branchId}/products";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CreateProductUseCase createProductUseCase;

    // The handler also needs the other use cases to be built.
    @MockitoBean
    private DeleteProductUseCase deleteProductUseCase;

    @MockitoBean
    private UpdateProductStockUseCase updateProductStockUseCase;

    private WebTestClient.RequestHeadersSpec<?> postJson(Object branchId, String body) {
        return webTestClient.post().uri(PATH, branchId).contentType(MediaType.APPLICATION_JSON).bodyValue(body);
    }

    @Test
    void createsTheProductAndAnswersWith201() {
        UUID id = UUID.randomUUID();
        when(createProductUseCase.execute(BRANCH_ID, "Hamburguesa", 10))
                .thenReturn(Mono.just(
                        Product.builder().id(id).branchId(BRANCH_ID).name("Hamburguesa").stock(10).build()));

        postJson(BRANCH_ID, "{\"name\":\"Hamburguesa\",\"stock\":10}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id.toString())
                .jsonPath("$.branchId").isEqualTo(BRANCH_ID.toString())
                .jsonPath("$.name").isEqualTo("Hamburguesa")
                .jsonPath("$.stock").isEqualTo(10);
    }

    @Test
    void answersWith400WhenTheNameIsInvalid() {
        when(createProductUseCase.execute(BRANCH_ID, "", 10)).thenReturn(Mono.error(new InvalidProductNameException()));

        postJson(BRANCH_ID, "{\"name\":\"\",\"stock\":10}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA")
                .jsonPath("$.message").isEqualTo("Product name must be between 1 and 120 characters")
                .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    void answersWith400WhenTheStockIsMissing() {
        when(createProductUseCase.execute(BRANCH_ID, "Hamburguesa", null))
                .thenReturn(Mono.error(new InvalidProductStockException()));

        postJson(BRANCH_ID, "{\"name\":\"Hamburguesa\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");
    }

    @Test
    void answersWith400WhenTheStockIsNegative() {
        when(createProductUseCase.execute(BRANCH_ID, "Hamburguesa", -1))
                .thenReturn(Mono.error(new InvalidProductStockException()));

        postJson(BRANCH_ID, "{\"name\":\"Hamburguesa\",\"stock\":-1}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");
    }

    @Test
    void answersWith400WhenTheBodyIsMissing() {
        when(createProductUseCase.execute(BRANCH_ID, null, null))
                .thenReturn(Mono.error(new InvalidProductNameException()));

        webTestClient.post()
                .uri(PATH, BRANCH_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");
    }

    @Test
    void answersWith400WhenTheJsonIsMalformed() {
        postJson(BRANCH_ID, "{\"name\": ")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");

        verifyNoInteractions(createProductUseCase);
    }

    @Test
    void answersWith400WhenTheNameIsNotAText() {
        postJson(BRANCH_ID, "{\"name\":123,\"stock\":1}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");

        verifyNoInteractions(createProductUseCase);
    }

    // None of these may reach the use case: a decimal must not be cut to a whole number and a text must not be
    // converted to one, and a number beyond the range of an integer cannot be stored.
    @ParameterizedTest
    @ValueSource(strings = {"1.7", "\"5\"", "\"abc\"", "3000000000", "true"})
    void answersWith400WhenTheStockIsNotAWholeNumber(String stock) {
        postJson(BRANCH_ID, "{\"name\":\"Hamburguesa\",\"stock\":" + stock + "}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");

        verifyNoInteractions(createProductUseCase);
    }

    @Test
    void answersWith400WhenTheBranchIdIsNotAUuid() {
        postJson("abc", "{\"name\":\"Hamburguesa\",\"stock\":10}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA")
                .jsonPath("$.message").isEqualTo("Branch id must be a valid UUID")
                .jsonPath("$.timestamp").isNotEmpty();

        verifyNoInteractions(createProductUseCase);
    }

    @Test
    void answersWith404WhenTheBranchDoesNotExist() {
        when(createProductUseCase.execute(BRANCH_ID, "Hamburguesa", 10))
                .thenReturn(Mono.error(new BranchNotFoundException(BRANCH_ID)));

        postJson(BRANCH_ID, "{\"name\":\"Hamburguesa\",\"stock\":10}")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BRANCH_NOT_FOUND")
                .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    void answersWith409WhenTheNameAlreadyExistsInTheBranch() {
        when(createProductUseCase.execute(BRANCH_ID, "Hamburguesa", 10))
                .thenReturn(Mono.error(new DuplicateProductNameException("Hamburguesa")));

        postJson(BRANCH_ID, "{\"name\":\"Hamburguesa\",\"stock\":10}")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("DUPLICATE_NAME")
                .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    void answersWith415WhenTheContentTypeIsNotJson() {
        webTestClient.post()
                .uri(PATH, BRANCH_ID)
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue("Hamburguesa")
                .exchange()
                .expectStatus().isEqualTo(415)
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNSUPPORTED_MEDIA_TYPE");

        verifyNoInteractions(createProductUseCase);
    }

    @Test
    void answersWith500WithoutLeakingInternalDetailsOnUnexpectedErrors() {
        when(createProductUseCase.execute(BRANCH_ID, "Hamburguesa", 10))
                .thenReturn(Mono.error(new IllegalStateException("connection refused to 10.0.0.5")));

        postJson(BRANCH_ID, "{\"name\":\"Hamburguesa\",\"stock\":10}")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INTERNAL_ERROR")
                .jsonPath("$.message").isEqualTo("Unexpected error")
                .jsonPath("$.timestamp").isNotEmpty();
    }
}
