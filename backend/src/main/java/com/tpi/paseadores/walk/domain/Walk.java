package com.tpi.paseadores.walk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// the offer a walker publishes: schedule, zone, meeting point, capacity and price per pet.
// No client_id: a group walk has many clients, derived through walk_pets -> pets -> owner_id.
// Free spots are never stored: they are counted from JOINED bookings (only FULL is denormalized).
@Entity
@Table(name = "walks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Walk {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // set once on publish: a walk never changes walker (RN-31 checks ownership against it)
    @Column(name = "walker_id", nullable = false, updatable = false)
    private UUID walkerId;

    @Column(name = "zone_id", nullable = false)
    private UUID zoneId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    // computed by Postgres from start_time + duration_minutes; read back after insert/update
    // and used to detect overlapping walks (RN-18)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "end_time", insertable = false, updatable = false)
    private LocalDateTime endTime;

    // only grows once published, never below the JOINED bookings (RN-20)
    @Column(nullable = false)
    private int capacity;

    // fixed once published (RN-19): no setter and not updatable
    @Column(name = "price_per_pet", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal pricePerPet;

    @Column(name = "meeting_point", nullable = false, columnDefinition = "text")
    private String meetingPoint;

    // maps to the Postgres enum type walk_status, not a plain varchar
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "walk_status")
    private WalkStatus status = WalkStatus.PUBLISHED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // business rules for publishing (RN-12 to RN-18) are checked by the service before calling this
    public Walk(UUID walkerId, UUID zoneId, LocalDateTime startTime, int durationMinutes, int capacity,
                BigDecimal pricePerPet, String meetingPoint) {
        this.walkerId = walkerId;
        this.zoneId = zoneId;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.capacity = capacity;
        this.pricePerPet = pricePerPet;
        this.meetingPoint = meetingPoint;
    }

    public boolean isOwnedBy(UUID userId) {
        return walkerId.equals(userId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return id != null && id.equals(((Walk) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
