package com.tpi.paseadores.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// one row per (walker, zone) the walker services; no id of its own, matches walker_zones' composite PK
@Entity
@Table(name = "walker_zones")
@IdClass(WalkerZoneId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalkerZone {
    @Id
    @Column(name = "walker_id")
    private UUID walkerId;

    @Id
    @Column(name = "zone_id")
    private UUID zoneId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public WalkerZone(UUID walkerId, UUID zoneId) {
        this.walkerId = walkerId;
        this.zoneId = zoneId;
    }
}
