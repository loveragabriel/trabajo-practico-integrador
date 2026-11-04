package com.tpi.paseadores.shared.exception;

// a request value is well-formed but not acceptable (e.g. page size out of range, "to" before "from").
// Answers 400 with the offending field in fieldErrors, same shape as Bean Validation errors.
public class InvalidFieldException extends RuntimeException {
    private final String field;

    public InvalidFieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
