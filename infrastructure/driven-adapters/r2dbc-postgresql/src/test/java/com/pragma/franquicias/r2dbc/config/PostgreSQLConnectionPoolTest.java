package com.pragma.franquicias.r2dbc.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.postgresql.client.SSLMode;
import org.junit.jupiter.api.Test;

class PostgreSQLConnectionPoolTest {

    private static PostgresqlConnectionProperties properties(boolean ssl) {
        return new PostgresqlConnectionProperties(
                "localhost", 5432, "dbName", "public", "username", "password", ssl, 2, 5);
    }

    @Test
    void buildsThePoolFromTheConnectionProperties() {
        ConnectionPool pool = new PostgreSQLConnectionPool().getConnectionConfig(properties(false));

        assertNotNull(pool);
    }

    @Test
    void buildsThePoolWhenTlsIsEnabled() {
        ConnectionPool pool = new PostgreSQLConnectionPool().getConnectionConfig(properties(true));

        assertNotNull(pool);
    }

    // RDS PostgreSQL 16 only accepts encrypted connections, locally the container does not use TLS.
    @Test
    void requiresTlsOnlyWhenTheSslPropertyIsEnabled() {
        assertEquals(SSLMode.REQUIRE, PostgreSQLConnectionPool.sslMode(properties(true)));
        assertEquals(SSLMode.DISABLE, PostgreSQLConnectionPool.sslMode(properties(false)));
    }

    @Test
    void tlsIsOffByDefault() {
        PostgresqlConnectionProperties defaults = new PostgresqlConnectionProperties(
                "localhost", 5432, "dbName", null, "username", "password", false, 2, 5);

        assertEquals(SSLMode.DISABLE, PostgreSQLConnectionPool.sslMode(defaults));
    }
}
