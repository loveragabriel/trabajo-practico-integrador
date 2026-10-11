package com.tpi.paseadores.shared.exception;

// the actor is authenticated but not allowed to act on this resource (e.g. someone else's pet)
public class ForbiddenActionException extends RuntimeException {
    public ForbiddenActionException(String message) {
        super(message);
    }
}
