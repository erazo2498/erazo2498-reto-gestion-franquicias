package com.pragma.franquicias.api.error;

import static java.util.Map.entry;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

import com.pragma.franquicias.model.branch.exceptions.BranchNotFoundException;
import com.pragma.franquicias.model.branch.exceptions.DuplicateBranchNameException;
import com.pragma.franquicias.model.branch.exceptions.InvalidBranchNameException;
import com.pragma.franquicias.model.franchise.exceptions.DuplicateFranchiseNameException;
import com.pragma.franquicias.model.franchise.exceptions.FranchiseNotFoundException;
import com.pragma.franquicias.model.franchise.exceptions.InvalidFranchiseNameException;
import com.pragma.franquicias.model.product.exceptions.DuplicateProductNameException;
import com.pragma.franquicias.model.product.exceptions.InvalidProductNameException;
import com.pragma.franquicias.model.product.exceptions.InvalidProductStockException;
import com.pragma.franquicias.model.product.exceptions.ProductNotFoundException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ApiErrorHandler {

    // What the client receives for an error: the HTTP status, a stable code and the message.
    private record ApiError(HttpStatus status, String code, Function<Throwable, String> message) {

        // The message is the one the exception carries.
        static ApiError of(HttpStatus status, String code) {
            return new ApiError(status, code, Throwable::getMessage);
        }

        // The message is fixed, so no internal detail of the error reaches the client.
        static ApiError fixed(HttpStatus status, String code, String message) {
            return new ApiError(status, code, error -> message);
        }
    }

    // One line per known error. The lookup is by exact class: these exceptions have no subclasses.
    // A new error is added here; any error that is not in the table is answered with a generic 500.
    private static final Map<Class<? extends Throwable>, ApiError> ERRORS = Map.ofEntries(
            entry(InvalidFranchiseNameException.class, ApiError.of(BAD_REQUEST, "INVALID_DATA")),
            entry(InvalidBranchNameException.class, ApiError.of(BAD_REQUEST, "INVALID_DATA")),
            entry(InvalidProductNameException.class, ApiError.of(BAD_REQUEST, "INVALID_DATA")),
            entry(InvalidProductStockException.class, ApiError.of(BAD_REQUEST, "INVALID_DATA")),
            entry(InvalidPathIdException.class, ApiError.of(BAD_REQUEST, "INVALID_DATA")),
            entry(ServerWebInputException.class, ApiError.fixed(BAD_REQUEST, "INVALID_DATA", "Malformed request body")),
            entry(FranchiseNotFoundException.class, ApiError.of(NOT_FOUND, "FRANCHISE_NOT_FOUND")),
            entry(BranchNotFoundException.class, ApiError.of(NOT_FOUND, "BRANCH_NOT_FOUND")),
            entry(ProductNotFoundException.class, ApiError.of(NOT_FOUND, "PRODUCT_NOT_FOUND")),
            entry(DuplicateFranchiseNameException.class, ApiError.of(CONFLICT, "DUPLICATE_NAME")),
            entry(DuplicateBranchNameException.class, ApiError.of(CONFLICT, "DUPLICATE_NAME")),
            entry(DuplicateProductNameException.class, ApiError.of(CONFLICT, "DUPLICATE_NAME")),
            entry(
                    UnsupportedMediaTypeStatusException.class,
                    ApiError.fixed(
                            UNSUPPORTED_MEDIA_TYPE,
                            "UNSUPPORTED_MEDIA_TYPE",
                            "Content type must be application/json")));

    private static final ApiError UNEXPECTED = ApiError.fixed(INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error");

    public Mono<ServerResponse> handle(Mono<ServerResponse> response) {
        return response.onErrorResume(error -> Optional.ofNullable(ERRORS.get(error.getClass()))
                .map(apiError -> body(apiError, error))
                .orElseGet(() -> unexpected(error)));
    }

    private Mono<ServerResponse> unexpected(Throwable error) {
        log.error("Unexpected error while handling the request", error);
        return body(UNEXPECTED, error);
    }

    private Mono<ServerResponse> body(ApiError apiError, Throwable error) {
        return ServerResponse.status(apiError.status())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ErrorResponse(apiError.code(), apiError.message().apply(error), Instant.now()));
    }
}
