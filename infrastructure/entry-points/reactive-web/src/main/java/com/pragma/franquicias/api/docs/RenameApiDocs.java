package com.pragma.franquicias.api.docs;

import static com.pragma.franquicias.api.docs.OpenApiSupport.componentsWithErrorSchema;
import static com.pragma.franquicias.api.docs.OpenApiSupport.error;
import static com.pragma.franquicias.api.docs.OpenApiSupport.jsonContent;
import static com.pragma.franquicias.api.docs.OpenApiSupport.mergePath;
import static com.pragma.franquicias.api.docs.OpenApiSupport.response;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// The router stays free of documentation annotations: the OpenAPI description of the three renames lives here.
// The response schemas (FranchiseResponse, BranchResponse, ProductResponse) are registered by the documentation of
// the endpoints that create those resources.
@Configuration
public class RenameApiDocs {

    private static final String FRANCHISE_ID = "3f2b8c1e-7a4d-4e59-9c0a-1d6e5b8f2a47";
    private static final String BRANCH_ID = "9a1c4e72-5b3d-4f08-8e61-2c7d0a9b3f15";
    private static final String PRODUCT_ID = "5d7e2a91-3c4b-4a6f-9b18-0e2f7c6d4a53";

    @Bean
    public OpenApiCustomizer renameDocs() {
        return openApi -> {
            registerSchemas(openApi);
            mergePath(
                    openApi,
                    "/api/v1/franchises/{franchiseId}",
                    item -> item.patch(operation(
                            "renameFranchise",
                            "Rename a franchise",
                            "Franchises",
                            List.of(pathId("franchiseId", "Identifier of the franchise.", FRANCHISE_ID)),
                            "FranchiseResponse",
                            map("id", FRANCHISE_ID, "name", "Pizza Co"),
                            "FRANCHISE_NOT_FOUND",
                            "Franchise '" + FRANCHISE_ID + "' does not exist",
                            "A franchise named 'Pizza Co' already exists",
                            "Franchise name must be between 1 and 120 characters")));
            mergePath(
                    openApi,
                    "/api/v1/franchises/{franchiseId}/branches/{branchId}",
                    item -> item.patch(operation(
                            "renameBranch",
                            "Rename a branch of a franchise",
                            "Branches",
                            List.of(
                                    pathId("franchiseId", "Identifier of the franchise.", FRANCHISE_ID),
                                    pathId("branchId", "Identifier of the branch.", BRANCH_ID)),
                            "BranchResponse",
                            map("id", BRANCH_ID, "franchiseId", FRANCHISE_ID, "name", "Sur"),
                            "BRANCH_NOT_FOUND",
                            "Branch '" + BRANCH_ID + "' does not exist",
                            "A branch named 'Sur' already exists in this franchise",
                            "Branch name must be between 1 and 120 characters")));
            mergePath(
                    openApi,
                    "/api/v1/branches/{branchId}/products/{productId}",
                    item -> item.patch(operation(
                            "renameProduct",
                            "Rename a product of a branch",
                            "Products",
                            List.of(
                                    pathId("branchId", "Identifier of the branch.", BRANCH_ID),
                                    pathId("productId", "Identifier of the product.", PRODUCT_ID)),
                            "ProductResponse",
                            map("id", PRODUCT_ID, "branchId", BRANCH_ID, "name", "Pizza", "stock", 10),
                            "PRODUCT_NOT_FOUND",
                            "Product '" + PRODUCT_ID + "' was not found in branch '" + BRANCH_ID + "'",
                            "A product named 'Pizza' already exists in this branch",
                            "Product name must be between 1 and 120 characters")));
        };
    }

    private void registerSchemas(OpenAPI openApi) {
        Components components = componentsWithErrorSchema(openApi);
        components.addSchemas("RenameRequest", renameRequestSchema());
    }

    private Schema<?> renameRequestSchema() {
        return new ObjectSchema()
                .description("The new name.")
                .addProperty(
                        "name",
                        new StringSchema()
                                .minLength(1)
                                .maxLength(120)
                                .description("New name. Surrounding spaces are removed; it must stay unique inside its "
                                        + "parent (franchises: globally), ignoring upper and lower case.")
                                .example("Pizza Co"))
                .required(List.of("name"));
    }

    private PathParameter pathId(String name, String description, String example) {
        PathParameter parameter = new PathParameter();
        parameter.name(name).description(description).required(true).schema(new UUIDSchema()).example(example);
        return parameter;
    }

    private Operation operation(
            String operationId,
            String summary,
            String tag,
            List<PathParameter> parameters,
            String responseSchema,
            Map<String, Object> renamed,
            String notFoundCode,
            String notFoundMessage,
            String duplicateMessage,
            String invalidMessage) {
        Operation operation = new Operation()
                .operationId(operationId)
                .summary(summary)
                .description("Replaces the name and returns the updated resource. The rename is scoped to its parent: a "
                        + "resource of another parent is never touched.")
                .addTagsItem(tag)
                .requestBody(new RequestBody()
                        .required(true)
                        .content(jsonContent("RenameRequest", "newName", "A valid new name", map("name", "Pizza Co"))))
                .responses(new ApiResponses()
                        .addApiResponse("200", response("Renamed", responseSchema, "renamed", "The renamed resource", renamed))
                        .addApiResponse("400", response(
                                "Invalid data: missing, blank or too long name, a name that is not text, malformed body "
                                        + "or an id that is not a UUID",
                                "ErrorResponse",
                                "invalidName",
                                "Blank name",
                                error("INVALID_DATA", invalidMessage)))
                        .addApiResponse("404", response(
                                "The resource does not exist under that parent",
                                "ErrorResponse",
                                "notFound",
                                "Unknown resource",
                                error(notFoundCode, notFoundMessage)))
                        .addApiResponse("409", response(
                                "Another resource of the same parent already has that name (ignoring case)",
                                "ErrorResponse",
                                "duplicateName",
                                "Name already used",
                                error("DUPLICATE_NAME", duplicateMessage)))
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
        parameters.forEach(operation::addParametersItem);
        return operation;
    }

    private Map<String, Object> map(Object... keysAndValues) {
        Map<String, Object> value = new LinkedHashMap<>();
        for (int index = 0; index < keysAndValues.length; index += 2) {
            value.put((String) keysAndValues[index], keysAndValues[index + 1]);
        }
        return value;
    }
}
