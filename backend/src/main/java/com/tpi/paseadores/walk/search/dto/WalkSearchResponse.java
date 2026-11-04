package com.tpi.paseadores.walk.search.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalkSearchResponse(
        UUID id, UUID zoneId, String zoneName, LocalDateTime startTime,
        int durationMinutes, String meetingPoint, BigDecimal pricePerPet,
        int capacity, int availableSpots, WalkerSummary walker) {
    public record WalkerSummary(UUID id, String name, BigDecimal ratingAvg, int reviewCount) {}
}
