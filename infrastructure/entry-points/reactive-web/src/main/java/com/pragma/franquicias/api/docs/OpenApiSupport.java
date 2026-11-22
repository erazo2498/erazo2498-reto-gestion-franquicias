package com.pragma.franquicias.api.docs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Pieces shared by the OpenAPI descriptions of every endpoint, so each endpoint only describes what is its own.
final class OpenApiSupport {

    private static final String JSON = "application/json";
    private static final String SAMPLE_TIMESTAMP = "2026-01-15T10:30:00Z";

    private OpenApiSupport() {
    }

    // Returns the components of the document, creating them if needed, with the error schema every endpoint uses.
    static Components componentsWithErrorSchema(OpenAPI openApi) {
        Components components = Optional.ofNullable(openApi.getComponents()).orElseGet(Components::new);
        components.addSchemas("ErrorResponse", errorResponseSchema());
        openApi.setComponents(components);
        return components;
    }

    private static Schema<?> errorResponseSchema() {
        return new ObjectSchema()
                .description("Error returned by every endpoint.")
                .addProperty("code", new StringSchema().description("Stable code that identifies the error."))
                .addProperty("message", new StringSchema().description("Human readable explanation."))
                .addProperty("timestamp", new DateTimeSchema().description("Moment the error was produced."))
                .required(List.of("code", "message", "timestamp"));
    }

    static ApiResponse response(
            String description, String schema, String exampleName, String exampleSummary, Object exampleValue) {
        return new ApiResponse()
                .description(description)
                .content(jsonContent(schema, exampleName, exampleSummary, exampleValue));
    }

    static Content jsonContent(String schema, String exampleName, String exampleSummary, Object exampleValue) {
        return new Content()
                .addMediaType(
                        JSON,
                        new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/" + schema))
                                .addExamples(exampleName, new Example().summary(exampleSummary).value(exampleValue)));
    }

    static Map<String, Object> error(String code, String message) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("code", code);
        value.put("message", message);
        value.put("timestamp", SAMPLE_TIMESTAMP);
        return value;
    }
}
