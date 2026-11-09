package com.tpi.paseadores.user.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// reemplaza el conjunto completo de zonas de cobertura, no un alta/baja incremental
public record WalkerCoverageRequest(
        @DecimalMin(value = "0", message = "must not be negative") @Digits(integer = 8, fraction = 2) BigDecimal defaultPrice,
        @Positive Integer defaultCapacity,
        @NotNull @Size(min = 1, message = "Elegí al menos un barrio de cobertura.") List<@NotNull UUID> zoneIds
) {
}
