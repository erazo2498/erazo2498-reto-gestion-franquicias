package com.pragma.franquicias.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import reactor.test.StepVerifier;

// Starts the application the way a real run does: ONLY the DB_* values (as they come from .env, docker compose or
// AWS Secrets Manager), so the JDBC url of Liquibase and the R2DBC settings that application.yaml builds from them are
// really exercised. The other integration tests override those settings with the container's own.
@SpringBootTest
class DatabaseSettingsFromEnvironmentIntegrationTest {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseValues(DynamicPropertyRegistry registry) {
        registry.add("DB_HOST", POSTGRES::getHost);
        registry.add("DB_PORT", () -> POSTGRES.getMappedPort(5432));
        registry.add("DB_NAME", POSTGRES::getDatabaseName);
        registry.add("DB_USERNAME", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);
        // DB_SSL is deliberately not set: the local database does not use TLS and the default must keep working.
    }

    @Autowired
    private DatabaseClient databaseClient;

    @Test
    void startsAndMigratesWithOnlyTheDbValuesAndNoTlsByDefault() {
        StepVerifier.create(databaseClient.sql("SELECT COUNT(*) AS total FROM databasechangelog")
                        .map(row -> row.get("total", Long.class))
                        .one())
                .expectNextMatches(total -> total >= 3)
                .verifyComplete();
    }
}
