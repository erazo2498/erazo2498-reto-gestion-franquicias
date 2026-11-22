package com.pragma.franquicias.api.error;

import com.pragma.franquicias.model.branch.exceptions.DuplicateBranchNameException;
import com.pragma.franquicias.model.branch.exceptions.InvalidBranchNameException;
import com.pragma.franquicias.model.franchise.exceptions.DuplicateFranchiseNameException;
import com.pragma.franquicias.model.franchise.exceptions.FranchiseNotFoundException;
import com.pragma.franquicias.model.franchise.exceptions.InvalidFranchiseNameException;
import java.time.Instant;
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

    public Mono<ServerResponse> handle(Mono<ServerResponse> response) {
        return response
                .onErrorResume(
                        InvalidFranchiseNameException.class,
                        error -> body(HttpStatus.BAD_REQUEST, "INVALID_DATA", error.getMessage()))
                .onErrorResume(
                        DuplicateFranchiseNameException.class,
                        error -> body(HttpStatus.CONFLICT, "DUPLICATE_NAME", error.getMessage()))
                .onErrorResume(
                        InvalidBranchNameException.class,
                        error -> body(HttpStatus.BAD_REQUEST, "INVALID_DATA", error.getMessage()))
                .onErrorResume(
                        InvalidPathIdException.class,
                        error -> body(HttpStatus.BAD_REQUEST, "INVALID_DATA", error.getMessage()))
                .onErrorResume(
                        FranchiseNotFoundException.class,
                        error -> body(HttpStatus.NOT_FOUND, "FRANCHISE_NOT_FOUND", error.getMessage()))
                .onErrorResume(
                        DuplicateBranchNameException.class,
                        error -> body(HttpStatus.CONFLICT, "DUPLICATE_NAME", error.getMessage()))
                .onErrorResume(
                        ServerWebInputException.class,
                        error -> body(HttpStatus.BAD_REQUEST, "INVALID_DATA", "Malformed request body"))
                .onErrorResume(
                        UnsupportedMediaTypeStatusException.class,
                        error -> body(
                                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                                "UNSUPPORTED_MEDIA_TYPE",
                                "Content type must be application/json"))
                .onErrorResume(this::unexpected);
    }

    private Mono<ServerResponse> unexpected(Throwable error) {
        log.error("Unexpected error while handling the request", error);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error");
    }

    private Mono<ServerResponse> body(HttpStatus status, String code, String message) {
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ErrorResponse(code, message, Instant.now()));
    }
}
