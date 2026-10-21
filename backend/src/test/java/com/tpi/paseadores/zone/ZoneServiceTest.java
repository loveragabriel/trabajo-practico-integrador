package com.tpi.paseadores.zone;

import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {
    @Mock
    private ZoneRepository repository;
    @InjectMocks
    private ZoneService service;

    @Test
    void returnsCatalogAsDtosInRepositoryOrder() {
        Zone zone = mock(Zone.class);
        UUID id = UUID.randomUUID();
        when(zone.getId()).thenReturn(id);
        when(zone.getName()).thenReturn("Palermo");
        when(zone.getCity()).thenReturn("Buenos Aires");
        when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(zone));

        assertThat(service.findAll()).containsExactly(new ZoneResponse(id, "Palermo", "Buenos Aires"));
    }

    @Test
    void returnsAnEmptyCatalogWithoutInventingZones() {
        when(repository.findAllByOrderByNameAsc()).thenReturn(List.of());
        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void returnsExistingZone() {
        UUID id = UUID.randomUUID();
        Zone zone = mock(Zone.class);
        when(zone.getId()).thenReturn(id);
        when(zone.getName()).thenReturn("Belgrano");
        when(zone.getCity()).thenReturn("Buenos Aires");
        when(repository.findById(id)).thenReturn(Optional.of(zone));

        assertThat(service.findById(id)).isEqualTo(new ZoneResponse(id, "Belgrano", "Buenos Aires"));
    }

    @Test
    void rejectsUnknownZone() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Zone not found");
    }

    @Test
    void reportsWhetherAZoneExists() {
        UUID known = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        when(repository.existsById(known)).thenReturn(true);
        when(repository.existsById(unknown)).thenReturn(false);

        assertThat(service.existsById(known)).isTrue();
        assertThat(service.existsById(unknown)).isFalse();
    }
}
