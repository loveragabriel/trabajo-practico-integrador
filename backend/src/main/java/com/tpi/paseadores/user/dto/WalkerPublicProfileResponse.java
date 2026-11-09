package com.tpi.paseadores.user.dto;

import com.tpi.paseadores.zone.dto.ZoneResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// consulta pública: sin email ni teléfono, a diferencia de UserResponse
public record WalkerPublicProfileResponse(
        UUID id,
        String fullName,
        BigDecimal defaultPrice,
        Integer defaultCapacity,
        BigDecimal ratingAvg,
        int reviewCount,
        List<ZoneResponse> zones
) {
}
