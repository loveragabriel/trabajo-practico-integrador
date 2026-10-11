package com.tpi.paseadores.shared.exception;

import java.time.Instant;
import java.util.List;

// uniform shape for every error response: validation, authorization, business or unexpected
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {
    }
}
