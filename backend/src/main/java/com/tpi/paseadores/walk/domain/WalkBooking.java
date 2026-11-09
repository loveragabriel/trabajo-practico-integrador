package com.tpi.paseadores.walk.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "walk_pets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalkBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID walkId;

    @Column(nullable = false, updatable = false)
    private UUID petId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "booking_status")
    private BookingStatus status = BookingStatus.JOINED;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    public WalkBooking(UUID walkId, UUID petId, LocalDateTime now) {
        this.walkId = walkId;
        this.petId = petId;
        this.joinedAt = now;
    }

    public void rejoin(LocalDateTime now) {
        status = BookingStatus.JOINED;
        joinedAt = now;
    }

    public void cancel() {
        status = BookingStatus.CANCELLED;
    }
}
