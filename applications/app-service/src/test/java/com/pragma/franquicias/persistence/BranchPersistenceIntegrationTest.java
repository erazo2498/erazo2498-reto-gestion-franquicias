package com.pragma.franquicias.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pragma.franquicias.model.branch.Branch;
import com.pragma.franquicias.model.branch.exceptions.DuplicateBranchNameException;
import com.pragma.franquicias.model.branch.gateways.SaveBranchGateway;
import com.pragma.franquicias.model.franchise.Franchise;
import com.pragma.franquicias.model.franchise.exceptions.FranchiseNotFoundException;
import com.pragma.franquicias.model.franchise.gateways.SaveFranchiseGateway;
import com.pragma.franquicias.support.PostgresIntegrationTest;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
class BranchPersistenceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private SaveBranchGateway saveBranchGateway;

    @Autowired
    private SaveFranchiseGateway saveFranchiseGateway;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    @AfterEach
    void cleanTables() {
        // Branches first: they reference franchises. Cleaning also after each test keeps other test classes unaffected.
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

    private static Branch branch(UUID franchiseId, String name) {
        return Branch.builder().id(UUID.randomUUID()).franchiseId(franchiseId).name(name).build();
    }

    @Test
    void savesTheBranchAndStoresItInTheDatabase() {
        Branch branch = branch(existingFranchise("Burger Co"), "Centro");

        StepVerifier.create(saveBranchGateway.save(branch))
                .assertNext(saved -> assertEquals(branch, saved))
                .verifyComplete();

        StepVerifier.create(databaseClient.sql("SELECT name FROM branch WHERE id = :id")
                        .bind("id", branch.getId())
                        .map(row -> row.get("name", String.class))
                        .one())
                .expectNext("Centro")
                .verifyComplete();
    }

    @Test
    void rejectsABranchOfAFranchiseThatDoesNotExist() {
        StepVerifier.create(saveBranchGateway.save(branch(UUID.randomUUID(), "Centro")))
                .expectError(FranchiseNotFoundException.class)
                .verify();
    }

    @Test
    void rejectsADuplicateNameInTheSameFranchiseIgnoringCase() {
        UUID franchiseId = existingFranchise("Burger Co");
        Mono<Branch> secondSave = saveBranchGateway.save(branch(franchiseId, "centro"));

        StepVerifier.create(saveBranchGateway.save(branch(franchiseId, "Centro")).then(secondSave))
                .expectError(DuplicateBranchNameException.class)
                .verify();
    }

    @Test
    void rejectsADuplicateNameWithAccentsAndEnyeIgnoringCase() {
        UUID franchiseId = existingFranchise("Burger Co");
        Mono<Branch> secondSave = saveBranchGateway.save(branch(franchiseId, "ÑANDÚ"));

        StepVerifier.create(saveBranchGateway.save(branch(franchiseId, "ñandú")).then(secondSave))
                .expectError(DuplicateBranchNameException.class)
                .verify();
    }

    @Test
    void allowsTheSameNameInDifferentFranchises() {
        UUID firstFranchise = existingFranchise("Burger Co");
        UUID secondFranchise = existingFranchise("Pizza Co");
        Mono<Branch> secondSave = saveBranchGateway.save(branch(secondFranchise, "Centro"));

        StepVerifier.create(saveBranchGateway.save(branch(firstFranchise, "Centro")).then(secondSave))
                .assertNext(saved -> assertEquals(secondFranchise, saved.getFranchiseId()))
                .verifyComplete();
    }

    @Test
    void onlyOneOfManySimultaneousCreationsWins() {
        UUID franchiseId = existingFranchise("Burger Co");
        int attempts = 10;
        Flux<String> outcomes = Flux.range(0, attempts)
                .flatMap(
                        attempt -> saveBranchGateway
                                .save(branch(franchiseId, "Centro"))
                                .map(saved -> "CREATED")
                                .onErrorResume(error -> Mono.just(error.getClass().getSimpleName())),
                        attempts);

        StepVerifier.create(outcomes.collectList())
                .assertNext(results -> {
                    assertEquals(1, Collections.frequency(results, "CREATED"));
                    assertEquals(attempts - 1, Collections.frequency(results, "DuplicateBranchNameException"));
                })
                .verifyComplete();
    }

    @Test
    void databaseItselfRejectsNamesLongerThanTheLimit() {
        UUID franchiseId = existingFranchise("Burger Co");

        StepVerifier.create(databaseClient.sql(
                                "INSERT INTO branch (id, franchise_id, name) VALUES (:id, :franchiseId, :name)")
                        .bind("id", UUID.randomUUID())
                        .bind("franchiseId", franchiseId)
                        .bind("name", "a".repeat(121))
                        .then())
                .expectError()
                .verify();
    }

    @Test
    void databaseRefusesToDeleteAFranchiseThatStillHasBranches() {
        UUID franchiseId = existingFranchise("Burger Co");
        StepVerifier.create(saveBranchGateway.save(branch(franchiseId, "Centro")))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(databaseClient.sql("DELETE FROM franchise WHERE id = :id")
                        .bind("id", franchiseId)
                        .then())
                .expectError()
                .verify();
    }

    @Test
    void recordsTheMigrationInTheLiquibaseHistory() {
        StepVerifier.create(databaseClient.sql(
                                "SELECT COUNT(*) AS total FROM databasechangelog WHERE id = '002-create-branch'")
                        .map(row -> row.get("total", Long.class))
                        .one())
                .expectNext(1L)
                .verifyComplete();
    }
}
