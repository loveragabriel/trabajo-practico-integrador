package com.tpi.paseadores.walk.dto;

import com.tpi.paseadores.walk.domain.WalkStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalkReadResponse(UUID id, UUID zoneId, String zoneName, LocalDateTime startTime,
                               LocalDateTime endTime, int durationMinutes, String meetingPoint,
                               BigDecimal pricePerPet, int capacity, int bookedSpots, int availableSpots,
                               WalkStatus status, WalkerSummary walker) {
    public record WalkerSummary(UUID id, String name, BigDecimal ratingAvg, int reviewCount) {}
}
