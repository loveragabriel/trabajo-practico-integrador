package com.tpi.paseadores.walk;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class WalkSearchService {
    private final WalkSearchRepository repository;
    private final Clock clock;

    @Autowired
    public WalkSearchService(WalkSearchRepository repository) {
        this(repository, Clock.system(ZoneId.of("America/Argentina/Buenos_Aires")));
    }

    WalkSearchService(WalkSearchRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public WalkSearchPageResponse search(WalkSearchFilters filters) {
        if (filters.page() < 0) throw new InvalidWalkSearchException("page", "La página no puede ser negativa.");
        if (filters.size() < 1 || filters.size() > 50) {
            throw new InvalidWalkSearchException("size", "El tamaño de página debe estar entre 1 y 50.");
        }
        if (filters.startsFrom() != null && filters.startsTo() != null
                && !filters.startsTo().isAfter(filters.startsFrom())) {
            throw new InvalidWalkSearchException("startsTo", "Hasta debe ser posterior a Desde.");
        }
        var rows = repository.findAvailable(filters, LocalDateTime.now(clock));
        boolean hasNext = rows.size() > filters.size();
        var items = List.copyOf(rows.subList(0, Math.min(filters.size(), rows.size())));
        return new WalkSearchPageResponse(items, filters.page(), filters.size(), hasNext);
    }
}
