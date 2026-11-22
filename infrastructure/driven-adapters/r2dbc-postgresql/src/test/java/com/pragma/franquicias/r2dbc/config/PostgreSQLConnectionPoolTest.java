package com.pragma.franquicias.r2dbc.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.r2dbc.pool.ConnectionPool;
import org.junit.jupiter.api.Test;

class PostgreSQLConnectionPoolTest {

    @Test
    void buildsThePoolFromTheConnectionProperties() {
        PostgresqlConnectionProperties properties =
                new PostgresqlConnectionProperties("localhost", 5432, "dbName", "public", "username", "password", 2, 5);

        ConnectionPool pool = new PostgreSQLConnectionPool().getConnectionConfig(properties);

        assertNotNull(pool);
    }
}
