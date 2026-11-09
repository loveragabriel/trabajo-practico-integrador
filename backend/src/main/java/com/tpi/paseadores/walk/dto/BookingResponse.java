package com.tpi.paseadores.walk.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookingResponse(UUID id, UUID walkId, UUID petId, String petName, String walkerName,
                              String zoneName, LocalDateTime startTime, LocalDateTime endTime,
                              String bookingStatus, String walkStatus, boolean canCancel) {}
