package com.tpi.paseadores.walk.dto;

import com.tpi.paseadores.walk.domain.BookingStatus;
import com.tpi.paseadores.walk.domain.WalkStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// una fila de "mis reservas": la reserva del cliente junto con los datos del paseo al que pertenece
public record ClientBookingResponse(UUID bookingId, BookingStatus bookingStatus, UUID petId, String petName,
                                     UUID walkId, WalkStatus walkStatus, LocalDateTime startTime,
                                     LocalDateTime endTime, String meetingPoint, BigDecimal pricePerPet,
                                     String zoneName, WalkerSummary walker) {
    public record WalkerSummary(UUID id, String name, BigDecimal ratingAvg, int reviewCount) {}
}
