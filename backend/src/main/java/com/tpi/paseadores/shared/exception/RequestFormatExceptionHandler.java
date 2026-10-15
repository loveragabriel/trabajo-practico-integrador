package com.tpi.paseadores.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

/** Complements the existing advice without replacing its business/security handlers. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestFormatExceptionHandler {
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Invalid parameter format", request,
                List.of(new ApiError.FieldError(ex.getName(), "Invalid format")));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex,
                                                           HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Required parameter is missing", request,
                List.of(new ApiError.FieldError(ex.getParameterName(), "Required")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Invalid request body", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupportedMethod(HttpRequestMethodNotSupportedException ex,
                                                           HttpServletRequest request) {
        var headers = new org.springframework.http.HttpHeaders();
        if (ex.getSupportedHttpMethods() != null) {
            headers.setAllow(ex.getSupportedHttpMethods());
        }
        var body = error(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", request, null);
        return new ResponseEntity<>(body, headers, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupportedMediaType(HttpServletRequest request) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type", request, null);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String message, HttpServletRequest request,
                                               List<ApiError.FieldError> fields) {
        return ResponseEntity.status(status).body(error(status, message, request, fields));
    }

    private ApiError error(HttpStatus status, String message, HttpServletRequest request,
                           List<ApiError.FieldError> fields) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message,
                request.getRequestURI(), fields);
    }
}
