package com.tpi.paseadores.walk.search;

import com.tpi.paseadores.shared.exception.InvalidFieldException;
import com.tpi.paseadores.walk.search.dto.WalkSearchFilters;
import com.tpi.paseadores.walk.search.dto.WalkSearchPageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class WalkSearchService {
    private final WalkSearchRepository repository;
    // CABA time, from shared/config/TimeConfig; tests pass a fixed clock
    private final Clock clock;

    public WalkSearchService(WalkSearchRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public WalkSearchPageResponse search(WalkSearchFilters filters) {
        if (filters.page() < 0) throw new InvalidFieldException("page", "La página no puede ser negativa.");
        if (filters.size() < 1 || filters.size() > 50) {
            throw new InvalidFieldException("size", "El tamaño de página debe estar entre 1 y 50.");
        }
        if (filters.startsFrom() != null && filters.startsTo() != null
                && !filters.startsTo().isAfter(filters.startsFrom())) {
            throw new InvalidFieldException("startsTo", "Hasta debe ser posterior a Desde.");
        }
        // the repository returns up to size + 1 rows: the extra one only tells whether a next page exists
        var rows = repository.findAvailable(filters, LocalDateTime.now(clock));
        boolean hasNext = rows.size() > filters.size();
        var items = List.copyOf(rows.subList(0, Math.min(filters.size(), rows.size())));
        return new WalkSearchPageResponse(items, filters.page(), filters.size(), hasNext);
    }
}
