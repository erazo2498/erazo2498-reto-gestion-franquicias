package com.pragma.franquicias.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pragma.franquicias.model.franchise.Franchise;
import com.pragma.franquicias.model.franchise.exceptions.DuplicateFranchiseNameException;
import com.pragma.franquicias.model.franchise.gateways.SaveFranchiseGateway;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
@Testcontainers
class FranchisePersistenceIntegrationTest {

    private static final int POSTGRES_PORT = 5432;

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("adapters.r2dbc.host", POSTGRES::getHost);
        registry.add("adapters.r2dbc.port", () -> POSTGRES.getMappedPort(POSTGRES_PORT));
        registry.add("adapters.r2dbc.database", POSTGRES::getDatabaseName);
        registry.add("adapters.r2dbc.username", POSTGRES::getUsername);
        registry.add("adapters.r2dbc.password", POSTGRES::getPassword);
        registry.add("spring.liquibase.url", POSTGRES::getJdbcUrl);
        registry.add("spring.liquibase.user", POSTGRES::getUsername);
        registry.add("spring.liquibase.password", POSTGRES::getPassword);
    }

    @Autowired
    private SaveFranchiseGateway saveFranchiseGateway;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    void cleanTable() {
        StepVerifier.create(databaseClient.sql("DELETE FROM franchise").then()).verifyComplete();
    }

    private static Franchise franchise(String name) {
        return Franchise.builder().id(UUID.randomUUID()).name(name).build();
    }

    @Test
    void savesTheFranchiseAndStoresItInTheDatabase() {
        Franchise franchise = franchise("Burger Co");

        StepVerifier.create(saveFranchiseGateway.save(franchise))
                .assertNext(saved -> assertEquals(franchise, saved))
                .verifyComplete();

        StepVerifier.create(databaseClient.sql("SELECT name FROM franchise WHERE id = :id")
                        .bind("id", franchise.getId())
                        .map(row -> row.get("name", String.class))
                        .one())
                .expectNext("Burger Co")
                .verifyComplete();
    }

    @Test
    void rejectsADuplicateNameIgnoringCase() {
        Mono<Franchise> secondSave = saveFranchiseGateway.save(franchise("burger co"));

        StepVerifier.create(saveFranchiseGateway.save(franchise("Burger Co")).then(secondSave))
                .expectError(DuplicateFranchiseNameException.class)
                .verify();
    }

    @Test
    void rejectsADuplicateNameWithAccentsAndEnyeIgnoringCase() {
        Mono<Franchise> secondSave = saveFranchiseGateway.save(franchise("ÑANDÚ"));

        StepVerifier.create(saveFranchiseGateway.save(franchise("ñandú")).then(secondSave))
                .expectError(DuplicateFranchiseNameException.class)
                .verify();
    }

    @Test
    void onlyOneOfManySimultaneousCreationsWins() {
        int attempts = 10;
        Flux<String> outcomes = Flux.range(0, attempts)
                .flatMap(
                        attempt -> saveFranchiseGateway
                                .save(franchise("Burger Co"))
                                .map(saved -> "CREATED")
                                .onErrorResume(error -> Mono.just(error.getClass().getSimpleName())),
                        attempts);

        StepVerifier.create(outcomes.collectList())
                .assertNext(results -> {
                    assertEquals(1, Collections.frequency(results, "CREATED"));
                    assertEquals(attempts - 1, Collections.frequency(results, "DuplicateFranchiseNameException"));
                })
                .verifyComplete();
    }

    @Test
    void databaseItselfRejectsNamesLongerThanTheLimit() {
        StepVerifier.create(databaseClient.sql("INSERT INTO franchise (id, name) VALUES (:id, :name)")
                        .bind("id", UUID.randomUUID())
                        .bind("name", "a".repeat(121))
                        .then())
                .expectError()
                .verify();
    }

    @Test
    void recordsTheMigrationInTheLiquibaseHistory() {
        StepVerifier.create(databaseClient.sql(
                                "SELECT COUNT(*) AS total FROM databasechangelog WHERE id = '001-create-franchise'")
                        .map(row -> row.get("total", Long.class))
                        .one())
                .expectNext(1L)
                .verifyComplete();
    }
}
