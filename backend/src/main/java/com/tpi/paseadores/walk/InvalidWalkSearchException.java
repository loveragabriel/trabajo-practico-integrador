package com.tpi.paseadores.walk;

public class InvalidWalkSearchException extends RuntimeException {
    private final String field;

    public InvalidWalkSearchException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
