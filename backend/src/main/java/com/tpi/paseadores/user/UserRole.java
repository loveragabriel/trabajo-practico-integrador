package com.tpi.paseadores.user;

// matches the Postgres enum user_role (OWNER, WALKER). The API-facing name for OWNER
// is "CLIENT" (frontend/docs wording); everything else, including the JWT role claim,
// uses this enum's own name.
public enum UserRole {
    OWNER, WALKER;

    public String toApiValue() {
        return this == OWNER ? "CLIENT" : "WALKER";
    }

    public static UserRole fromApiValue(String value) {
        return switch (value) {
            case "CLIENT" -> OWNER;
            case "WALKER" -> WALKER;
            default -> throw new IllegalArgumentException("Unknown role: " + value);
        };
    }
}
