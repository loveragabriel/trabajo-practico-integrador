package com.tpi.paseadores.auth.dto;

import com.tpi.paseadores.user.User;

import java.util.UUID;

public record UserSummary(UUID id, String name, String role) {
    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getFullName(), user.getRole().toApiValue());
    }
}
