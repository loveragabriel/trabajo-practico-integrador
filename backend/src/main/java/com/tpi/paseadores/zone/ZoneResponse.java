package com.tpi.paseadores.zone;

import java.util.UUID;

public record ZoneResponse(UUID id, String name, String city) {
    static ZoneResponse from(Zone zone) {
        return new ZoneResponse(zone.getId(), zone.getName(), zone.getCity());
    }
}
