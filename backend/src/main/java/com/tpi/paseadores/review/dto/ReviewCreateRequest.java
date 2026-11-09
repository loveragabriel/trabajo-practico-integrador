package com.tpi.paseadores.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// el walkId viene del path (/api/walks/{walkId}/reviews), no se repite acá
public record ReviewCreateRequest(
        @NotNull @Min(1) @Max(5) int rating, @Size(max = 120) String comment
) {
}
