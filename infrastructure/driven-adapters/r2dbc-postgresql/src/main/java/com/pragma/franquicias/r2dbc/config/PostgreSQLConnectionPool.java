package com.pragma.franquicias.r2dbc.config;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.client.SSLMode;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PostgresqlConnectionProperties.class)
public class PostgreSQLConnectionPool {

    private static final int MAX_IDLE_TIME_MINUTES = 30;

    @Bean
    public ConnectionPool getConnectionConfig(PostgresqlConnectionProperties properties) {
        PostgresqlConnectionConfiguration dbConfiguration = PostgresqlConnectionConfiguration.builder()
                .host(properties.host())
                .port(properties.port())
                .database(properties.database())
                .schema(properties.schema())
                .username(properties.username())
                .password(properties.password())
                .sslMode(sslMode(properties))
                .build();

        ConnectionPoolConfiguration poolConfiguration = ConnectionPoolConfiguration.builder()
                .connectionFactory(new PostgresqlConnectionFactory(dbConfiguration))
                .name("api-postgres-connection-pool")
                .initialSize(properties.poolInitialSize())
                .maxSize(properties.poolMaxSize())
                .maxIdleTime(Duration.ofMinutes(MAX_IDLE_TIME_MINUTES))
                .validationQuery("SELECT 1")
                .build();

        return new ConnectionPool(poolConfiguration);
    }

    // RDS PostgreSQL 16 only accepts encrypted connections; the local container does not use TLS. REQUIRE encrypts the
    // connection but does not verify the server certificate (a declared limitation: it protects against eavesdropping,
    // not against an impostor server).
    static SSLMode sslMode(PostgresqlConnectionProperties properties) {
        return properties.ssl() ? SSLMode.REQUIRE : SSLMode.DISABLE;
    }
}
