package com.tpi.paseadores.user.dto;

import com.tpi.paseadores.user.domain.User;
import com.tpi.paseadores.user.domain.UserRole;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        UserRole role,
        UUID zoneId,
        boolean active,
        BigDecimal defaultPrice,
        Integer defaultCapacity,
        BigDecimal ratingAvg,
        int reviewCount,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getZoneId(),
                user.isActive(),
                user.getDefaultPrice(),
                user.getDefaultCapacity(),
                user.getRatingAvg(),
                user.getReviewCount(),
                user.getCreatedAt()
        );
    }
}
