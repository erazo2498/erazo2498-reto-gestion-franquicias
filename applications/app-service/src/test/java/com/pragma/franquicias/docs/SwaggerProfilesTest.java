package com.pragma.franquicias.docs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

class SwaggerProfilesTest {

    @Configuration
    static class EmptyConfiguration {
    }

    @ParameterizedTest
    @CsvSource({
        "local,true",
        "dev,true",
        "staging,true",
        "prod,false",
        "none,false"
    })
    void enablesTheDocumentationOnlyInTheNonProductionProfiles(String profile, String expected) {
        String[] profiles = "none".equals(profile) ? new String[0] : new String[] {profile};

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(EmptyConfiguration.class)
                .web(WebApplicationType.NONE)
                .profiles(profiles)
                .run()) {
            assertEquals(expected, context.getEnvironment().getProperty("springdoc.api-docs.enabled", "false"));
            assertEquals(expected, context.getEnvironment().getProperty("springdoc.swagger-ui.enabled", "false"));
        }
    }
}
