package com.tpi.paseadores.walk.repository;

import com.tpi.paseadores.walk.domain.BookingStatus;
import com.tpi.paseadores.walk.domain.WalkBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalkBookingRepository extends JpaRepository<WalkBooking, UUID> {
    List<WalkBooking> findAllByWalkIdAndStatus(UUID walkId, BookingStatus status);
    List<WalkBooking> findAllByWalkIdAndPetIdIn(UUID walkId, List<UUID> petIds);
    Optional<WalkBooking> findByIdAndWalkId(UUID id, UUID walkId);
    long countByWalkIdAndStatus(UUID walkId, BookingStatus status);
}
