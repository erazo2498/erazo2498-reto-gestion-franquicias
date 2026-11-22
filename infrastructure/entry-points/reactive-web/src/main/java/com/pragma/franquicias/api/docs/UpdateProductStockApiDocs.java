package com.pragma.franquicias.api.docs;

import static com.pragma.franquicias.api.docs.OpenApiSupport.componentsWithErrorSchema;
import static com.pragma.franquicias.api.docs.OpenApiSupport.error;
import static com.pragma.franquicias.api.docs.OpenApiSupport.jsonContent;
import static com.pragma.franquicias.api.docs.OpenApiSupport.response;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.UUIDSchema;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// The router stays free of documentation annotations: the OpenAPI description of the endpoint lives here.
@Configuration
public class UpdateProductStockApiDocs {

    private static final String PATH = "/api/v1/branches/{branchId}/products/{productId}/stock";
    private static final String SAMPLE_BRANCH_ID = "9a1c4e72-5b3d-4f08-8e61-2c7d0a9b3f15";
    private static final String SAMPLE_PRODUCT_ID = "5d7e2a91-3c4b-4a6f-9b18-0e2f7c6d4a53";

    @Bean
    public OpenApiCustomizer updateProductStockDocs() {
        return openApi -> {
            registerSchemas(openApi);
            openApi.path(PATH, new PathItem().patch(updateProductStockOperation()));
        };
    }

    private void registerSchemas(OpenAPI openApi) {
        Components components = componentsWithErrorSchema(openApi);
        components.addSchemas("UpdateProductStockRequest", updateProductStockRequestSchema());
        components.addSchemas("ProductResponse", CreateProductApiDocs.productResponseSchema());
    }

    private Schema<?> updateProductStockRequestSchema() {
        return new ObjectSchema()
                .description("The new stock of the product.")
                .addProperty(
                        "stock",
                        new IntegerSchema()
                                .minimum(BigDecimal.ZERO)
                                .description("New stock: a whole number greater than or equal to 0. It replaces the "
                                        + "current value; decimals and text are rejected, not converted.")
                                .example(25))
                .required(List.of("stock"));
    }

    private Operation updateProductStockOperation() {
        return new Operation()
                .operationId("updateProductStock")
                .summary("Set the stock of a product")
                .description("Replaces the stock of the product, only if it belongs to that branch, and returns the "
                        + "updated product. Sending the same value again leaves the same state. With simultaneous "
                        + "requests the last write wins.")
                .addTagsItem("Products")
                .addParametersItem(new PathParameter()
                        .name("branchId")
                        .description("Identifier of the branch the product belongs to.")
                        .required(true)
                        .schema(new UUIDSchema())
                        .example(SAMPLE_BRANCH_ID))
                .addParametersItem(new PathParameter()
                        .name("productId")
                        .description("Identifier of the product.")
                        .required(true)
                        .schema(new UUIDSchema())
                        .example(SAMPLE_PRODUCT_ID))
                .requestBody(new RequestBody()
                        .required(true)
                        .content(jsonContent("UpdateProductStockRequest", "validStock", "A valid stock", request())))
                .responses(new ApiResponses()
                        .addApiResponse("200", response(
                                "Stock updated", "ProductResponse", "updated", "The updated product", updated()))
                        .addApiResponse("400", response(
                                "Invalid data: missing, negative, decimal or text stock, malformed body, or an id that "
                                        + "is not a UUID",
                                "ErrorResponse",
                                "invalidStock",
                                "Negative stock",
                                error(
                                        "INVALID_DATA",
                                        "Product stock is required and must be a whole number greater than or equal "
                                                + "to 0")))
                        .addApiResponse("404", response(
                                "The product is not in that branch: it does not exist, it belongs to another branch "
                                        + "or the branch does not exist",
                                "ErrorResponse",
                                "productNotFound",
                                "Unknown product",
                                error(
                                        "PRODUCT_NOT_FOUND",
                                        "Product '" + SAMPLE_PRODUCT_ID + "' was not found in branch '"
                                                + SAMPLE_BRANCH_ID + "'")))
                        .addApiResponse("415", response(
                                "The request is not application/json",
                                "ErrorResponse",
                                "unsupportedMediaType",
                                "Wrong content type",
                                error("UNSUPPORTED_MEDIA_TYPE", "Content type must be application/json")))
                        .addApiResponse("500", response(
                                "Unexpected error; the details are only written to the server log",
                                "ErrorResponse",
                                "unexpectedError",
                                "Unexpected failure",
                                error("INTERNAL_ERROR", "Unexpected error"))));
    }

    private Map<String, Object> request() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("stock", 25);
        return value;
    }

    private Map<String, Object> updated() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", SAMPLE_PRODUCT_ID);
        value.put("branchId", SAMPLE_BRANCH_ID);
        value.put("name", "Hamburguesa");
        value.put("stock", 25);
        return value;
    }
}
