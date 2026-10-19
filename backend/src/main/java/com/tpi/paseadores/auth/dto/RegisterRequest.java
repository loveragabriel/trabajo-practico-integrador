package com.tpi.paseadores.auth.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

// defaultPrice/defaultCapacity only apply when role is WALKER (RN-04); the service
// ignores them for an OWNER instead of rejecting the request.
public record RegisterRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @Size(max = 30) String phone,
        @NotBlank @Pattern(regexp = "CLIENT|WALKER", message = "must be CLIENT or WALKER") String role,
        @NotNull UUID zoneId,
        @DecimalMin(value = "0", message = "must not be negative") BigDecimal defaultPrice,
        @Positive Integer defaultCapacity
) {
}
