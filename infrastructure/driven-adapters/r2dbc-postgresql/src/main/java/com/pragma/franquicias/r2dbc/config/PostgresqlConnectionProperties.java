package com.pragma.franquicias.r2dbc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "adapters.r2dbc")
public record PostgresqlConnectionProperties(
        String host,
        Integer port,
        String database,
        @DefaultValue("public") String schema,
        String username,
        String password,
        @DefaultValue("false") boolean ssl,
        @DefaultValue("2") int poolInitialSize,
        @DefaultValue("5") int poolMaxSize) {
}
