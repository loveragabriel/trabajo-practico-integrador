package com.tpi.paseadores.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ReviewCreateRequest(
        UUID walkId, @NotNull @Min(1) @Max(5) Integer rating, @Size(max = 120) String comment
) {
}
