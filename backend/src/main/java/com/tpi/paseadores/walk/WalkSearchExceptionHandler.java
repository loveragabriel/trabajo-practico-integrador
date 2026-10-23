package com.tpi.paseadores.walk;

import com.tpi.paseadores.shared.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Order(10)
public class WalkSearchExceptionHandler {
    @ExceptionHandler(InvalidWalkSearchException.class)
    public ResponseEntity<ApiError> invalidSearch(InvalidWalkSearchException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "Bad Request",
                ex.getMessage(), request.getRequestURI(),
                List.of(new ApiError.FieldError(ex.getField(), ex.getMessage()))));
    }
}
