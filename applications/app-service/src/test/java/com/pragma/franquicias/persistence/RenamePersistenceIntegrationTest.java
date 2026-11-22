package com.pragma.franquicias.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pragma.franquicias.model.branch.Branch;
import com.pragma.franquicias.model.branch.exceptions.DuplicateBranchNameException;
import com.pragma.franquicias.model.branch.gateways.RenameBranchGateway;
import com.pragma.franquicias.model.branch.gateways.SaveBranchGateway;
import com.pragma.franquicias.model.franchise.Franchise;
import com.pragma.franquicias.model.franchise.exceptions.DuplicateFranchiseNameException;
import com.pragma.franquicias.model.franchise.gateways.RenameFranchiseGateway;
import com.pragma.franquicias.model.franchise.gateways.SaveFranchiseGateway;
import com.pragma.franquicias.model.product.Product;
import com.pragma.franquicias.model.product.exceptions.DuplicateProductNameException;
import com.pragma.franquicias.model.product.gateways.RenameProductGateway;
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
class RenamePersistenceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private RenameFranchiseGateway renameFranchiseGateway;

    @Autowired
    private RenameBranchGateway renameBranchGateway;

    @Autowired
    private RenameProductGateway renameProductGateway;

    @Autowired
    private SaveFranchiseGateway saveFranchiseGateway;

    @Autowired
    private SaveBranchGateway saveBranchGateway;

    @Autowired
    private SaveProductGateway saveProductGateway;

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

    private UUID franchise(String name) {
        UUID id = UUID.randomUUID();
        StepVerifier.create(saveFranchiseGateway.save(Franchise.builder().id(id).name(name).build()))
                .expectNextCount(1)
                .verifyComplete();
        return id;
    }

    private UUID branch(UUID franchiseId, String name) {
        UUID id = UUID.randomUUID();
        StepVerifier.create(saveBranchGateway.save(
                        Branch.builder().id(id).franchiseId(franchiseId).name(name).build()))
                .expectNextCount(1)
                .verifyComplete();
        return id;
    }

    private UUID product(UUID branchId, String name, int stock) {
        UUID id = UUID.randomUUID();
        StepVerifier.create(saveProductGateway.save(
                        Product.builder().id(id).branchId(branchId).name(name).stock(stock).build()))
                .expectNextCount(1)
                .verifyComplete();
        return id;
    }

    private void assertStoredName(String table, UUID id, String expected) {
        StepVerifier.create(databaseClient.sql("SELECT name FROM " + table + " WHERE id = :id")
                        .bind("id", id)
                        .map(row -> row.get("name", String.class))
                        .one())
                .expectNext(expected)
                .verifyComplete();
    }

    // ---- franchise

    @Test
    void renamesAFranchise() {
        UUID id = franchise("Burger Co");

        StepVerifier.create(renameFranchiseGateway.rename(id, "Pizza Co"))
                .assertNext(renamed -> assertEquals("Pizza Co", renamed.getName()))
                .verifyComplete();

        assertStoredName("franchise", id, "Pizza Co");
    }

    @Test
    void aFranchiseCanChangeOnlyTheCaseOfItsOwnName() {
        UUID id = franchise("Burger Co");

        StepVerifier.create(renameFranchiseGateway.rename(id, "BURGER CO"))
                .assertNext(renamed -> assertEquals("BURGER CO", renamed.getName()))
                .verifyComplete();
    }

    @Test
    void rejectsRenamingAFranchiseToTheNameOfAnotherIgnoringCase() {
        franchise("Burger Co");
        UUID other = franchise("Pizza Co");

        StepVerifier.create(renameFranchiseGateway.rename(other, "burger co"))
                .expectError(DuplicateFranchiseNameException.class)
                .verify();

        assertStoredName("franchise", other, "Pizza Co");
    }

    @Test
    void completesEmptyWhenTheFranchiseDoesNotExist() {
        StepVerifier.create(renameFranchiseGateway.rename(UUID.randomUUID(), "Pizza Co")).verifyComplete();
    }

    // ---- branch

    @Test
    void renamesABranchOfItsFranchise() {
        UUID franchiseId = franchise("Burger Co");
        UUID id = branch(franchiseId, "Centro");

        StepVerifier.create(renameBranchGateway.rename(franchiseId, id, "Sur"))
                .assertNext(renamed -> {
                    assertEquals("Sur", renamed.getName());
                    assertEquals(franchiseId, renamed.getFranchiseId());
                })
                .verifyComplete();

        assertStoredName("branch", id, "Sur");
    }

    @Test
    void rejectsRenamingABranchToTheNameOfAnotherBranchOfTheSameFranchise() {
        UUID franchiseId = franchise("Burger Co");
        branch(franchiseId, "Centro");
        UUID other = branch(franchiseId, "Norte");

        StepVerifier.create(renameBranchGateway.rename(franchiseId, other, "CENTRO"))
                .expectError(DuplicateBranchNameException.class)
                .verify();
    }

    @Test
    void aBranchCanTakeTheNameOfABranchOfAnotherFranchise() {
        UUID first = franchise("Burger Co");
        UUID second = franchise("Pizza Co");
        branch(first, "Centro");
        UUID id = branch(second, "Norte");

        StepVerifier.create(renameBranchGateway.rename(second, id, "Centro"))
                .assertNext(renamed -> assertEquals("Centro", renamed.getName()))
                .verifyComplete();
    }

    @Test
    void doesNotRenameABranchThatBelongsToAnotherFranchise() {
        UUID first = franchise("Burger Co");
        UUID second = franchise("Pizza Co");
        UUID id = branch(second, "Norte");

        StepVerifier.create(renameBranchGateway.rename(first, id, "Sur")).verifyComplete();

        assertStoredName("branch", id, "Norte");
    }

    // ---- product

    @Test
    void renamesAProductAndKeepsItsStock() {
        UUID branchId = branch(franchise("Burger Co"), "Centro");
        UUID id = product(branchId, "Hamburguesa", 10);

        StepVerifier.create(renameProductGateway.rename(branchId, id, "Pizza"))
                .assertNext(renamed -> {
                    assertEquals("Pizza", renamed.getName());
                    assertEquals(10, renamed.getStock());
                })
                .verifyComplete();

        assertStoredName("product", id, "Pizza");
    }

    @Test
    void rejectsRenamingAProductToTheNameOfAnotherProductOfTheSameBranch() {
        UUID branchId = branch(franchise("Burger Co"), "Centro");
        product(branchId, "Hamburguesa", 10);
        UUID other = product(branchId, "Gaseosa", 5);

        StepVerifier.create(renameProductGateway.rename(branchId, other, "HAMBURGUESA"))
                .expectError(DuplicateProductNameException.class)
                .verify();

        assertStoredName("product", other, "Gaseosa");
    }

    @Test
    void doesNotRenameAProductThatBelongsToAnotherBranch() {
        UUID franchiseId = franchise("Burger Co");
        UUID first = branch(franchiseId, "Centro");
        UUID second = branch(franchiseId, "Norte");
        UUID id = product(second, "Hamburguesa", 10);

        StepVerifier.create(renameProductGateway.rename(first, id, "Pizza")).verifyComplete();

        assertStoredName("product", id, "Hamburguesa");
    }
}
