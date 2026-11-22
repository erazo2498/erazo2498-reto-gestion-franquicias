package com.pragma.franquicias.api.error;

import java.time.Instant;

public record ErrorResponse(String code, String message, Instant timestamp) {
}
