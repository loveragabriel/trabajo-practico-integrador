package com.tpi.paseadores.walk;

import java.time.LocalDateTime;
import java.util.UUID;

public record WalkSearchFilters(UUID zoneId, LocalDateTime startsFrom, LocalDateTime startsTo,
                                int page, int size) {}
