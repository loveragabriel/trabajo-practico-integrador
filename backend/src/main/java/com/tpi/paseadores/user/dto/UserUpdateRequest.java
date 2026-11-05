package com.tpi.paseadores.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

// no incluye role ni email: el rol no se edita (RN-02) y el email no se reasigna en el MVP
public record UserUpdateRequest(
        @NotBlank @Size(max = 150) String fullName,
        @Size(max = 30) String phone,
        @NotNull UUID zoneId
) {
}
