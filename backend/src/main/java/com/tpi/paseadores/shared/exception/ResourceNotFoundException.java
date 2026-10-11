package com.tpi.paseadores.shared.exception;

// the requested entity does not exist (e.g. an unknown pet or walk id)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
