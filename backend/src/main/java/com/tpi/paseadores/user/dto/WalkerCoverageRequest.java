package com.tpi.paseadores.user.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// reemplaza el conjunto completo de zonas de cobertura, no un alta/baja incremental
public record WalkerCoverageRequest(
        @DecimalMin(value = "0", message = "must not be negative") BigDecimal defaultPrice,
        @Positive Integer defaultCapacity,
        @NotNull List<UUID> zoneIds
) {
}
