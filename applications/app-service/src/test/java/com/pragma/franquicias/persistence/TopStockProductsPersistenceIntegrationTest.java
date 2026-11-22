package com.pragma.franquicias.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pragma.franquicias.model.branch.Branch;
import com.pragma.franquicias.model.branch.BranchTopProduct;
import com.pragma.franquicias.model.branch.gateways.SaveBranchGateway;
import com.pragma.franquicias.model.franchise.Franchise;
import com.pragma.franquicias.model.franchise.gateways.ExistsFranchiseGateway;
import com.pragma.franquicias.model.franchise.gateways.SaveFranchiseGateway;
import com.pragma.franquicias.model.product.Product;
import com.pragma.franquicias.model.product.gateways.FindTopStockProductsGateway;
import com.pragma.franquicias.model.product.gateways.SaveProductGateway;
import com.pragma.franquicias.support.PostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.test.StepVerifier;

@SpringBootTest
class TopStockProductsPersistenceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private FindTopStockProductsGateway findTopStockProductsGateway;

    @Autowired
    private ExistsFranchiseGateway existsFranchiseGateway;

    @Autowired
    private SaveProductGateway saveProductGateway;

    @Autowired
    private SaveBranchGateway saveBranchGateway;

    @Autowired
    private SaveFranchiseGateway saveFranchiseGateway;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    @AfterEach
    void cleanTables() {
        // Children first: products reference branches and branches reference franchises.
        StepVerifier.create(databaseClient.sql("DELETE FROM product").then()).verifyComplete();
        StepVerifier.create(databaseClient.sql("DELETE FROM branch").then()).verifyComplete();
        StepVerifier.create(databaseClient.sql("DELETE FROM franchise").then()).verifyComplete();
    }

    private UUID existingFranchise(String name) {
        UUID id = UUID.randomUUID();
        StepVerifier.create(saveFranchiseGateway.save(Franchise.builder().id(id).name(name).build()))
                .expectNextCount(1)
                .verifyComplete();
        return id;
    }

    private UUID existingBranch(UUID franchiseId, String name) {
        UUID id = UUID.randomUUID();
        StepVerifier.create(saveBranchGateway.save(
                        Branch.builder().id(id).franchiseId(franchiseId).name(name).build()))
                .expectNextCount(1)
                .verifyComplete();
        return id;
    }

    private void existingProduct(UUID branchId, String name, int stock) {
        StepVerifier.create(saveProductGateway.save(
                        Product.builder().id(UUID.randomUUID()).branchId(branchId).name(name).stock(stock).build()))
                .expectNextCount(1)
                .verifyComplete();
    }

    private static String topOf(BranchTopProduct row) {
        return row.getBranchName() + ":" + row.getProduct().map(p -> p.getName() + "=" + p.getStock()).orElse("-");
    }

    @Test
    void tellsWhetherAFranchiseExists() {
        UUID franchiseId = existingFranchise("Burger Co");

        StepVerifier.create(existsFranchiseGateway.existsById(franchiseId)).expectNext(true).verifyComplete();
        StepVerifier.create(existsFranchiseGateway.existsById(UUID.randomUUID())).expectNext(false).verifyComplete();
    }

    @Test
    void returnsTheProductOfHighestStockOfEachBranchOrderedByBranchName() {
        UUID franchiseId = existingFranchise("Burger Co");
        UUID norte = existingBranch(franchiseId, "norte");
        UUID centro = existingBranch(franchiseId, "Centro");
        existingProduct(centro, "Hamburguesa", 40);
        existingProduct(centro, "Gaseosa", 5);
        existingProduct(norte, "Papas", 7);
        existingProduct(norte, "Pizza", 90);

        StepVerifier.create(findTopStockProductsGateway.findByFranchise(franchiseId).map(row -> topOf(row)))
                .expectNext("Centro:Hamburguesa=40")
                .expectNext("norte:Pizza=90")
                .verifyComplete();
    }

    @Test
    void theRowCarriesTheBranchAndTheFullProduct() {
        UUID franchiseId = existingFranchise("Burger Co");
        UUID branchId = existingBranch(franchiseId, "Centro");
        existingProduct(branchId, "Hamburguesa", 40);

        StepVerifier.create(findTopStockProductsGateway.findByFranchise(franchiseId))
                .assertNext(row -> {
                    assertEquals(branchId, row.getBranchId());
                    assertEquals("Centro", row.getBranchName());
                    assertEquals(branchId, row.getProduct().orElseThrow().getBranchId());
                    assertEquals("Hamburguesa", row.getProduct().orElseThrow().getName());
                })
                .verifyComplete();
    }

    @Test
    void breaksATieByTheNameIgnoringCase() {
        UUID franchiseId = existingFranchise("Burger Co");
        UUID branchId = existingBranch(franchiseId, "Centro");
        existingProduct(branchId, "Zeta", 10);
        existingProduct(branchId, "alfa", 10);
        existingProduct(branchId, "Beta", 10);

        StepVerifier.create(findTopStockProductsGateway.findByFranchise(franchiseId).map(row -> topOf(row)))
                .expectNext("Centro:alfa=10")
                .verifyComplete();
    }

    @Test
    void includesABranchWithoutProductsWithAnEmptyProduct() {
        UUID franchiseId = existingFranchise("Burger Co");
        UUID withProducts = existingBranch(franchiseId, "Centro");
        existingBranch(franchiseId, "Norte");
        existingProduct(withProducts, "Hamburguesa", 40);

        StepVerifier.create(findTopStockProductsGateway.findByFranchise(franchiseId).map(row -> topOf(row)))
                .expectNext("Centro:Hamburguesa=40")
                .expectNext("Norte:-")
                .verifyComplete();
    }

    @Test
    void aProductWithZeroStockStillCounts() {
        UUID franchiseId = existingFranchise("Burger Co");
        UUID branchId = existingBranch(franchiseId, "Centro");
        existingProduct(branchId, "Agotado", 0);

        StepVerifier.create(findTopStockProductsGateway.findByFranchise(franchiseId).map(row -> topOf(row)))
                .expectNext("Centro:Agotado=0")
                .verifyComplete();
    }

    @Test
    void doesNotMixBranchesOrProductsOfOtherFranchises() {
        UUID burger = existingFranchise("Burger Co");
        UUID pizza = existingFranchise("Pizza Co");
        UUID burgerBranch = existingBranch(burger, "Centro");
        UUID pizzaBranch = existingBranch(pizza, "Centro");
        existingProduct(burgerBranch, "Hamburguesa", 40);
        existingProduct(pizzaBranch, "Pizza", 500);

        StepVerifier.create(findTopStockProductsGateway.findByFranchise(burger).map(row -> topOf(row)))
                .expectNext("Centro:Hamburguesa=40")
                .verifyComplete();
    }

    @Test
    void completesEmptyWhenTheFranchiseHasNoBranches() {
        UUID franchiseId = existingFranchise("Burger Co");

        StepVerifier.create(findTopStockProductsGateway.findByFranchise(franchiseId)).verifyComplete();
    }
}
