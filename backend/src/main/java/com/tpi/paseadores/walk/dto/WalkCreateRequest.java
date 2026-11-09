package com.tpi.paseadores.walk.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalkCreateRequest(
        @NotNull UUID zoneId,
        @NotNull LocalDateTime startTime,
        @NotNull @Min(30) @JsonDeserialize(using = WholeNumber.class) Integer durationMinutes,
        @NotNull @Positive @JsonDeserialize(using = WholeNumber.class) Integer capacity,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal pricePerPet,
        @NotBlank String meetingPoint
) {
    public static class WholeNumber extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return context.reportInputMismatch(Integer.class, "Expected a whole number");
            }
            return parser.getIntValue();
        }
    }
}
