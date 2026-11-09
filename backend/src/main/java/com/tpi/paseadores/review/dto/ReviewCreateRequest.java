package com.tpi.paseadores.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReviewCreateRequest(
        @NotNull UUID walkId, @NotNull @Min(1) @Max(5) int rating, @Max(120) String comment
) {
}
