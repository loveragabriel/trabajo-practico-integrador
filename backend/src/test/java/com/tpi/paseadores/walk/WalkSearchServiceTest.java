package com.tpi.paseadores.walk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalkSearchServiceTest {
    private WalkSearchRepository repository;
    private WalkSearchService service;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 23, 10, 0);

    @BeforeEach
    void setUp() {
        repository = mock(WalkSearchRepository.class);
        service = new WalkSearchService(repository, Clock.fixed(Instant.parse("2026-10-23T13:00:00Z"),
                ZoneId.of("America/Argentina/Buenos_Aires")));
    }

    @Test
    void usesCabaTimeAndKeepsOnlyTheRequestedPage() {
        var filters = new WalkSearchFilters(null, null, null, 0, 1);
        var row = new WalkSearchResponse(UUID.randomUUID(), UUID.randomUUID(), "Palermo", NOW.plusDays(1),
                60, "Entrada de la plaza", BigDecimal.TEN, 4, 3,
                new WalkSearchResponse.WalkerSummary(UUID.randomUUID(), "Paseador", BigDecimal.ZERO, 0));
        when(repository.findAvailable(filters, NOW)).thenReturn(List.of(row, row));
        var result = service.search(filters);
        assertThat(result.items()).containsExactly(row);
        assertThat(result.hasNext()).isTrue();
        verify(repository).findAvailable(filters, NOW);
    }

    @Test
    void emptySearchHasNoNextPage() {
        var filters = new WalkSearchFilters(null, null, null, 0, 12);
        when(repository.findAvailable(filters, NOW)).thenReturn(List.of());
        var result = service.search(filters);
        assertThat(result.items()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void rejectsEqualAndReversedRangesBeforeQuerying() {
        for (var end : List.of(NOW, NOW.minusHours(1))) {
            assertThatThrownBy(() -> service.search(new WalkSearchFilters(null, NOW, end, 0, 12)))
                    .isInstanceOf(InvalidWalkSearchException.class);
        }
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsUnboundedOrNegativePagination() {
        for (var filters : List.of(new WalkSearchFilters(null, null, null, -1, 12),
                new WalkSearchFilters(null, null, null, 0, 0),
                new WalkSearchFilters(null, null, null, 0, 51))) {
            assertThatThrownBy(() -> service.search(filters)).isInstanceOf(InvalidWalkSearchException.class);
        }
        verifyNoInteractions(repository);
    }
}
