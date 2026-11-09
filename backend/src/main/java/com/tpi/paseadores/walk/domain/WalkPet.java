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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

// the booking itself: a client's pet joining a published walk. Its own entity, not a plain
// join table (it carries status and joined_at, and status evolves after creation).
@Entity
@Table(name = "walk_pets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalkPet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "walk_id", nullable = false, updatable = false)
    private UUID walkId;

    @Column(name = "pet_id", nullable = false, updatable = false)
    private UUID petId;

    // maps to the Postgres enum type booking_status, not a plain varchar
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "booking_status")
    private BookingStatus status = BookingStatus.JOINED;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    public WalkPet(UUID walkId, UUID petId, LocalDateTime joinedAt) {
        this.walkId = walkId;
        this.petId = petId;
        this.joinedAt = joinedAt;
    }

    // RN-26: cancelled and re-joined reuses the same row (the UNIQUE(walk_id, pet_id) forbids a new one)
    public void rejoin(LocalDateTime now) {
        this.status = BookingStatus.JOINED;
        this.joinedAt = now;
    }

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return id != null && id.equals(((WalkPet) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
