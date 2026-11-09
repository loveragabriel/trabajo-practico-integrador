package com.tpi.paseadores.walk.dto;

import com.tpi.paseadores.walk.domain.BookingStatus;
import com.tpi.paseadores.walk.domain.WalkPet;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookingResponse(UUID id, UUID petId, String petName, BookingStatus status, LocalDateTime joinedAt) {
    public static BookingResponse from(WalkPet booking, String petName) {
        return new BookingResponse(booking.getId(), booking.getPetId(), petName, booking.getStatus(),
                booking.getJoinedAt());
    }
}
