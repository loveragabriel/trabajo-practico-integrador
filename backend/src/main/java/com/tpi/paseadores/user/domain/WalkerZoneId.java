package com.tpi.paseadores.user.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class WalkerZoneId implements Serializable {
    private UUID walkerId;
    private UUID zoneId;

    public WalkerZoneId() {
        // required by JPA
    }

    public WalkerZoneId(UUID walkerId, UUID zoneId) {
        this.walkerId = walkerId;
        this.zoneId = zoneId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WalkerZoneId that)) {
            return false;
        }
        return Objects.equals(walkerId, that.walkerId) && Objects.equals(zoneId, that.zoneId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(walkerId, zoneId);
    }
}
