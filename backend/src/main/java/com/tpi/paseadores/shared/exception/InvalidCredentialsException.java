package com.tpi.paseadores.shared.exception;

// login failed: unknown email or wrong password. Same message/status for both,
// so the response never reveals whether an email is registered.
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
