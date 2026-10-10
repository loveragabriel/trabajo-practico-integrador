package com.tpi.paseadores.shared.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService("test-secret-not-for-production-use");

    @Test
    void generatesAndParsesValidToken() {
        String token = jwtService.generateToken("user-123", "WALKER");

        var claims = jwtService.parseClaims(token);

        assertEquals("user-123", claims.getSubject());
        assertEquals("WALKER", claims.get("role", String.class));
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtService otherService = new JwtService("a-totally-different-secret");
        String token = otherService.generateToken("user-123", "OWNER");

        assertThrows(JwtException.class, () -> jwtService.parseClaims(token));
    }
}
