package com.pragma.franquicias.api.franchise;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pragma.franquicias.api.config.JacksonConfig;
import com.pragma.franquicias.api.error.ApiErrorHandler;
import com.pragma.franquicias.model.branch.BranchTopProduct;
import com.pragma.franquicias.model.franchise.exceptions.FranchiseNotFoundException;
import com.pragma.franquicias.model.product.Product;
import com.pragma.franquicias.usecase.createfranchise.CreateFranchiseUseCase;
import com.pragma.franquicias.usecase.gettopstockproducts.GetTopStockProductsUseCase;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

@WebFluxTest
@ContextConfiguration(
        classes = {FranchiseRouter.class, FranchiseHandler.class, ApiErrorHandler.class, JacksonConfig.class})
class GetTopStockProductsRouteTest {

    private static final UUID FRANCHISE_ID = UUID.randomUUID();
    private static final String PATH = "/api/v1/franchises/{franchiseId}/top-stock-products";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private GetTopStockProductsUseCase getTopStockProductsUseCase;

    // The handler also needs the create use case to be built.
    @MockitoBean
    private CreateFranchiseUseCase createFranchiseUseCase;

    @Test
    void answersWith200AndOneRowPerBranchWithItsTopProduct() {
        UUID centro = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        BranchTopProduct withProduct = BranchTopProduct.builder()
                .branchId(centro)
                .branchName("Centro")
                .product(Optional.of(
                        Product.builder().id(productId).branchId(centro).name("Hamburguesa").stock(40).build()))
                .build();
        when(getTopStockProductsUseCase.execute(FRANCHISE_ID)).thenReturn(Flux.just(withProduct));

        webTestClient.get().uri(PATH, FRANCHISE_ID).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].branchId").isEqualTo(centro.toString())
                .jsonPath("$[0].branchName").isEqualTo("Centro")
                .jsonPath("$[0].product.id").isEqualTo(productId.toString())
                .jsonPath("$[0].product.name").isEqualTo("Hamburguesa")
                .jsonPath("$[0].product.stock").isEqualTo(40);
    }

    @Test
    void aBranchWithoutProductsAppearsWithANullProduct() {
        BranchTopProduct empty = BranchTopProduct.builder()
                .branchId(UUID.randomUUID())
                .branchName("Norte")
                .product(Optional.empty())
                .build();
        when(getTopStockProductsUseCase.execute(FRANCHISE_ID)).thenReturn(Flux.just(empty));

        webTestClient.get().uri(PATH, FRANCHISE_ID).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].branchName").isEqualTo("Norte")
                .jsonPath("$[0].product").isEmpty();
    }

    @Test
    void answersWith200AndAnEmptyListWhenTheFranchiseHasNoBranches() {
        when(getTopStockProductsUseCase.execute(FRANCHISE_ID)).thenReturn(Flux.empty());

        webTestClient.get().uri(PATH, FRANCHISE_ID).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    void answersWith404WhenTheFranchiseDoesNotExist() {
        when(getTopStockProductsUseCase.execute(FRANCHISE_ID))
                .thenReturn(Flux.error(new FranchiseNotFoundException(FRANCHISE_ID)));

        webTestClient.get().uri(PATH, FRANCHISE_ID).exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("FRANCHISE_NOT_FOUND")
                .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    void answersWith400WhenTheFranchiseIdIsNotAUuid() {
        webTestClient.get().uri(PATH, "abc").exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA")
                .jsonPath("$.message").isEqualTo("Franchise id must be a valid UUID");

        verifyNoInteractions(getTopStockProductsUseCase);
    }

    @Test
    void answersWith500WithoutLeakingInternalDetailsOnUnexpectedErrors() {
        when(getTopStockProductsUseCase.execute(FRANCHISE_ID))
                .thenReturn(Flux.error(new IllegalStateException("connection refused to 10.0.0.5")));

        webTestClient.get().uri(PATH, FRANCHISE_ID).exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INTERNAL_ERROR")
                .jsonPath("$.message").isEqualTo("Unexpected error")
                .jsonPath("$.timestamp").isNotEmpty();
    }
}
