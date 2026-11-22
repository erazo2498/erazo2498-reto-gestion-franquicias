package com.pragma.franquicias.api.docs;

import static com.pragma.franquicias.api.docs.OpenApiSupport.componentsWithErrorSchema;
import static com.pragma.franquicias.api.docs.OpenApiSupport.error;
import static com.pragma.franquicias.api.docs.OpenApiSupport.mergePath;
import static com.pragma.franquicias.api.docs.OpenApiSupport.response;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.UUIDSchema;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// The router stays free of documentation annotations: the OpenAPI description of the endpoint lives here.
@Configuration
public class DeleteProductApiDocs {

    private static final String PATH = "/api/v1/branches/{branchId}/products/{productId}";
    private static final String SAMPLE_BRANCH_ID = "9a1c4e72-5b3d-4f08-8e61-2c7d0a9b3f15";
    private static final String SAMPLE_PRODUCT_ID = "5d7e2a91-3c4b-4a6f-9b18-0e2f7c6d4a53";

    @Bean
    public OpenApiCustomizer deleteProductDocs() {
        return openApi -> {
            componentsWithErrorSchema(openApi);
            mergePath(openApi, PATH, item -> item.delete(deleteProductOperation()));
        };
    }

    private Operation deleteProductOperation() {
        return new Operation()
                .operationId("deleteProduct")
                .summary("Delete a product of a branch")
                .description("Deletes the product only if it belongs to that branch; a product of another branch is "
                        + "never touched. Deleting is idempotent: the answer is the same whether the product "
                        + "existed or not, so there is no 404.")
                .addTagsItem("Products")
                .addParametersItem(new PathParameter()
                        .name("branchId")
                        .description("Identifier of the branch the product belongs to.")
                        .required(true)
                        .schema(new UUIDSchema())
                        .example(SAMPLE_BRANCH_ID))
                .addParametersItem(new PathParameter()
                        .name("productId")
                        .description("Identifier of the product to delete.")
                        .required(true)
                        .schema(new UUIDSchema())
                        .example(SAMPLE_PRODUCT_ID))
                .responses(new ApiResponses()
                        .addApiResponse(
                                "204",
                                new ApiResponse()
                                        .description("The product is no longer in the branch. Same answer if it was "
                                                + "already gone, or if it belonged to another branch (nothing is deleted)."))
                        .addApiResponse("400", response(
                                "A branch id or a product id that is not a UUID",
                                "ErrorResponse",
                                "invalidId",
                                "Invalid product id",
                                error("INVALID_DATA", "Product id must be a valid UUID")))
                        .addApiResponse("500", response(
                                "Unexpected error; the details are only written to the server log",
                                "ErrorResponse",
                                "unexpectedError",
                                "Unexpected failure",
                                error("INTERNAL_ERROR", "Unexpected error"))));
    }
}
