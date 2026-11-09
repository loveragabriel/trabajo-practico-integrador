package com.tpi.paseadores.zone.dto;

import com.tpi.paseadores.zone.domain.Zone;

import java.util.UUID;

public record ZoneResponse(UUID id, String name, String city) {
    public static ZoneResponse from(Zone zone) {
        return new ZoneResponse(zone.getId(), zone.getName(), zone.getCity());
    }
}
