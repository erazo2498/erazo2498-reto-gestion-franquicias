package com.pragma.franquicias.api.product;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pragma.franquicias.api.config.JacksonConfig;
import com.pragma.franquicias.api.error.ApiErrorHandler;
import com.pragma.franquicias.usecase.createproduct.CreateProductUseCase;
import com.pragma.franquicias.usecase.deleteproduct.DeleteProductUseCase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest
@ContextConfiguration(
        classes = {ProductRouter.class, ProductHandler.class, ApiErrorHandler.class, JacksonConfig.class})
class DeleteProductRouteTest {

    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final String PATH = "/api/v1/branches/{branchId}/products/{productId}";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private DeleteProductUseCase deleteProductUseCase;

    // The router also needs the create use case to build the handler.
    @MockitoBean
    private CreateProductUseCase createProductUseCase;

    private WebTestClient.ResponseSpec delete(Object branchId, Object productId) {
        return webTestClient.delete().uri(PATH, branchId, productId).exchange();
    }

    @Test
    void deletesTheProductAndAnswersWith204WithoutABody() {
        when(deleteProductUseCase.execute(BRANCH_ID, PRODUCT_ID)).thenReturn(Mono.empty());

        delete(BRANCH_ID, PRODUCT_ID)
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        verify(deleteProductUseCase).execute(BRANCH_ID, PRODUCT_ID);
    }

    @Test
    void answersWith400WhenTheBranchIdIsNotAUuid() {
        delete("abc", PRODUCT_ID)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA")
                .jsonPath("$.message").isEqualTo("Branch id must be a valid UUID")
                .jsonPath("$.timestamp").isNotEmpty();

        verifyNoInteractions(deleteProductUseCase);
    }

    @Test
    void answersWith400WhenTheProductIdIsNotAUuid() {
        delete(BRANCH_ID, "abc")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA")
                .jsonPath("$.message").isEqualTo("Product id must be a valid UUID")
                .jsonPath("$.timestamp").isNotEmpty();

        verifyNoInteractions(deleteProductUseCase);
    }

    @Test
    void answersWith500WithoutLeakingInternalDetailsOnUnexpectedErrors() {
        when(deleteProductUseCase.execute(BRANCH_ID, PRODUCT_ID))
                .thenReturn(Mono.error(new IllegalStateException("connection refused to 10.0.0.5")));

        delete(BRANCH_ID, PRODUCT_ID)
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INTERNAL_ERROR")
                .jsonPath("$.message").isEqualTo("Unexpected error")
                .jsonPath("$.timestamp").isNotEmpty();
    }
}
