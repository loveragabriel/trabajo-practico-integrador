package com.tpi.paseadores.shared.exception;

// a domain rule was broken (e.g. walk is full, overlapping schedule)
public class BusinessRuleViolationException extends RuntimeException {
    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
