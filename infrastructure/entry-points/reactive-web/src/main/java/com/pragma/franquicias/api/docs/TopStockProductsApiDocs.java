package com.pragma.franquicias.api.docs;

import static com.pragma.franquicias.api.docs.OpenApiSupport.componentsWithErrorSchema;
import static com.pragma.franquicias.api.docs.OpenApiSupport.error;
import static com.pragma.franquicias.api.docs.OpenApiSupport.response;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// The router stays free of documentation annotations: the OpenAPI description of the endpoint lives here.
@Configuration
public class TopStockProductsApiDocs {

    private static final String PATH = "/api/v1/franchises/{franchiseId}/top-stock-products";
    private static final String SAMPLE_FRANCHISE_ID = "3f2b8c1e-7a4d-4e59-9c0a-1d6e5b8f2a47";

    @Bean
    public OpenApiCustomizer topStockProductsDocs() {
        return openApi -> {
            registerSchemas(openApi);
            openApi.path(PATH, new PathItem().get(topStockProductsOperation()));
        };
    }

    private void registerSchemas(OpenAPI openApi) {
        Components components = componentsWithErrorSchema(openApi);
        components.addSchemas("TopStockProductResponse", topStockProductSchema());
    }

    private Schema<?> topStockProductSchema() {
        // OpenAPI 3.1 says "an object or null" with a list of types; there is no "nullable" keyword.
        Schema<?> product = new Schema<>()
                .types(new LinkedHashSet<>(List.of("object", "null")))
                .description("The product of highest stock of the branch; null when the branch has no products.")
                .addProperty("id", new UUIDSchema())
                .addProperty("name", new StringSchema())
                .addProperty("stock", new IntegerSchema());
        return new ObjectSchema()
                .description("A branch with its product of highest stock.")
                .addProperty("branchId", new UUIDSchema())
                .addProperty("branchName", new StringSchema())
                .addProperty("product", product)
                .required(List.of("branchId", "branchName"));
    }

    private Operation topStockProductsOperation() {
        return new Operation()
                .operationId("getTopStockProducts")
                .summary("Product with the highest stock of each branch of a franchise")
                .description("Returns one row per branch of the franchise, ordered by branch name, with its product of "
                        + "highest stock. A tie is broken by the product name (ignoring case) and then by its id. A "
                        + "branch without products is included with a null product. A franchise without branches "
                        + "answers with an empty list.")
                .addTagsItem("Franchises")
                .addParametersItem(new PathParameter()
                        .name("franchiseId")
                        .description("Identifier of the franchise.")
                        .required(true)
                        .schema(new UUIDSchema())
                        .example(SAMPLE_FRANCHISE_ID))
                .responses(new ApiResponses()
                        .addApiResponse("200", listResponse())
                        .addApiResponse("400", response(
                                "A franchise id that is not a UUID",
                                "ErrorResponse",
                                "invalidId",
                                "Invalid franchise id",
                                error("INVALID_DATA", "Franchise id must be a valid UUID")))
                        .addApiResponse("404", response(
                                "The franchise does not exist",
                                "ErrorResponse",
                                "franchiseNotFound",
                                "Unknown franchise",
                                error("FRANCHISE_NOT_FOUND", "Franchise '" + SAMPLE_FRANCHISE_ID + "' does not exist")))
                        .addApiResponse("500", response(
                                "Unexpected error; the details are only written to the server log",
                                "ErrorResponse",
                                "unexpectedError",
                                "Unexpected failure",
                                error("INTERNAL_ERROR", "Unexpected error"))));
    }

    private ApiResponse listResponse() {
        return new ApiResponse()
                .description("One row per branch with its product of highest stock")
                .content(new Content()
                        .addMediaType(
                                "application/json",
                                new MediaType()
                                        .schema(new ArraySchema()
                                                .items(new Schema<>().$ref("#/components/schemas/TopStockProductResponse")))
                                        .addExamples(
                                                "topProducts",
                                                new Example()
                                                        .summary("A branch with a top product and an empty branch")
                                                        .value(List.of(centro(), norte())))));
    }

    private Map<String, Object> centro() {
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("id", "5d7e2a91-3c4b-4a6f-9b18-0e2f7c6d4a53");
        product.put("name", "Hamburguesa");
        product.put("stock", 40);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("branchId", "9a1c4e72-5b3d-4f08-8e61-2c7d0a9b3f15");
        row.put("branchName", "Centro");
        row.put("product", product);
        return row;
    }

    private Map<String, Object> norte() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("branchId", "c1f6a3d8-2e7b-4b90-8a54-6d3e9f0b7c21");
        row.put("branchName", "Norte");
        row.put("product", null);
        return row;
    }
}
