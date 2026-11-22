package com.pragma.franquicias.usecase.gettopstockproducts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pragma.franquicias.model.branch.BranchTopProduct;
import com.pragma.franquicias.model.franchise.exceptions.FranchiseNotFoundException;
import com.pragma.franquicias.model.franchise.gateways.ExistsFranchiseGateway;
import com.pragma.franquicias.model.product.Product;
import com.pragma.franquicias.model.product.gateways.FindTopStockProductsGateway;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class GetTopStockProductsUseCaseTest {

    private static final UUID FRANCHISE_ID = UUID.randomUUID();

    @Mock
    private ExistsFranchiseGateway existsFranchiseGateway;

    @Mock
    private FindTopStockProductsGateway findTopStockProductsGateway;

    private GetTopStockProductsUseCase useCase() {
        return new GetTopStockProductsUseCase(existsFranchiseGateway, findTopStockProductsGateway);
    }

    private static BranchTopProduct withProduct(String branchName, String productName, int stock) {
        UUID branchId = UUID.randomUUID();
        Product product =
                Product.builder().id(UUID.randomUUID()).branchId(branchId).name(productName).stock(stock).build();
        return BranchTopProduct.builder()
                .branchId(branchId)
                .branchName(branchName)
                .product(Optional.of(product))
                .build();
    }

    private static BranchTopProduct withoutProduct(String branchName) {
        return BranchTopProduct.builder()
                .branchId(UUID.randomUUID())
                .branchName(branchName)
                .product(Optional.empty())
                .build();
    }

    @Test
    void returnsTheTopProductOfEachBranchInTheOrderTheGatewayGivesThem() {
        BranchTopProduct centro = withProduct("Centro", "Hamburguesa", 40);
        BranchTopProduct norte = withoutProduct("Norte");
        when(existsFranchiseGateway.existsById(FRANCHISE_ID)).thenReturn(Mono.just(true));
        when(findTopStockProductsGateway.findByFranchise(FRANCHISE_ID)).thenReturn(Flux.just(centro, norte));

        StepVerifier.create(useCase().execute(FRANCHISE_ID))
                .assertNext(first -> assertEquals(40, first.getProduct().orElseThrow().getStock()))
                .assertNext(second -> assertEquals(Optional.empty(), second.getProduct()))
                .verifyComplete();

        verify(findTopStockProductsGateway).findByFranchise(FRANCHISE_ID);
    }

    @Test
    void completesEmptyWhenTheFranchiseHasNoBranches() {
        when(existsFranchiseGateway.existsById(FRANCHISE_ID)).thenReturn(Mono.just(true));
        when(findTopStockProductsGateway.findByFranchise(FRANCHISE_ID)).thenReturn(Flux.empty());

        StepVerifier.create(useCase().execute(FRANCHISE_ID)).verifyComplete();
    }

    @Test
    void failsWithFranchiseNotFoundWithoutQueryingWhenTheFranchiseDoesNotExist() {
        when(existsFranchiseGateway.existsById(FRANCHISE_ID)).thenReturn(Mono.just(false));

        StepVerifier.create(useCase().execute(FRANCHISE_ID))
                .expectError(FranchiseNotFoundException.class)
                .verify();

        verifyNoInteractions(findTopStockProductsGateway);
    }

    @Test
    void propagatesGatewayErrors() {
        when(existsFranchiseGateway.existsById(FRANCHISE_ID)).thenReturn(Mono.just(true));
        when(findTopStockProductsGateway.findByFranchise(FRANCHISE_ID))
                .thenReturn(Flux.error(new IllegalStateException("connection lost")));

        StepVerifier.create(useCase().execute(FRANCHISE_ID))
                .expectError(IllegalStateException.class)
                .verify();
    }
}
