package com.pragma.franquicias.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

public abstract class PostgresIntegrationTest {

    private static final int POSTGRES_PORT = 5432;

    // One container for the whole test run: it is started once and removed by Testcontainers when the JVM ends.
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

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
}
