package com.tpi.paseadores.auth.dto;

public record AuthResponse(String token, UserSummary user) {
}
