package com.pragma.franquicias.api.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

// By default Jackson converts values silently to the type a field expects. The API must reject them instead,
// so that what is stored is exactly what the client sent.
@Configuration
public class JacksonConfig {

    // {"name": 123} must not become the text "123".
    @Bean
    public JsonMapperBuilderCustomizer strictTextCustomizer() {
        return builder -> builder.withCoercionConfig(LogicalType.Textual, config -> config
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }

    // {"stock": 1.7} must not be cut to 1 and {"stock": "5"} must not become the number 5.
    @Bean
    public JsonMapperBuilderCustomizer strictIntegerCustomizer() {
        return builder -> builder.withCoercionConfig(LogicalType.Integer, config -> config
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.String, CoercionAction.Fail));
    }
}
