package com.pragma.franquicias.r2dbc.resilience;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ResilienceProperties.class)
public class ResilienceConfig {

    @Bean
    public ResilientOperations resilientOperations(ResilienceProperties properties) {
        return new ResilientOperations(properties);
    }
}
