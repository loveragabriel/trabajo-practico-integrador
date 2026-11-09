package com.tpi.paseadores.user.service;

import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.user.domain.User;
import com.tpi.paseadores.user.domain.UserRole;
import com.tpi.paseadores.user.domain.WalkerZone;
import com.tpi.paseadores.user.dto.WalkerCoverageRequest;
import com.tpi.paseadores.user.dto.WalkerCoverageResponse;
import com.tpi.paseadores.user.dto.WalkerPublicProfileResponse;
import com.tpi.paseadores.user.repository.UserRepository;
import com.tpi.paseadores.user.repository.WalkerZoneRepository;
import com.tpi.paseadores.zone.dto.ZoneResponse;
import com.tpi.paseadores.zone.service.ZoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalkerCoverageServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private WalkerZoneRepository walkerZoneRepository;
    @Mock
    private ZoneService zoneService;

    private WalkerCoverageService service;

    @BeforeEach
    void setUp() {
        service = new WalkerCoverageService(userRepository, walkerZoneRepository, zoneService);
    }

    @Test
    void returnsOwnCoverageOfAnActiveWalker() {
        User walker = walker(UUID.randomUUID(), true);
        UUID zoneId = UUID.randomUUID();
        when(userRepository.findById(walker.getId())).thenReturn(Optional.of(walker));
        when(walkerZoneRepository.findAllByWalkerId(walker.getId()))
                .thenReturn(List.of(new WalkerZone(walker.getId(), zoneId)));
        when(zoneService.findAllById(List.of(zoneId)))
                .thenReturn(List.of(new ZoneResponse(zoneId, "Palermo", "Buenos Aires")));

        WalkerCoverageResponse response = service.getOwnCoverage(walker.getId());

        assertThat(response.defaultPrice()).isEqualByComparingTo("2500");
        assertThat(response.zones()).extracting(ZoneResponse::name).containsExactly("Palermo");
    }

    @Test
    void rejectsCoverageQueryFromAClient() {
        User client = client(UUID.randomUUID());
        when(userRepository.findById(client.getId())).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.getOwnCoverage(client.getId()))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("Only walkers can manage service coverage");
    }

    @Test
    void reportsAnUnknownUserAsNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOwnCoverage(userId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void replacesThePriceCapacityAndWholeZoneSet() {
        User walker = walker(UUID.randomUUID(), true);
        UUID zoneId = UUID.randomUUID();
        when(userRepository.findById(walker.getId())).thenReturn(Optional.of(walker));
        when(userRepository.findByIdForUpdate(walker.getId())).thenReturn(Optional.of(walker));
        when(zoneService.existsById(zoneId)).thenReturn(true);
        var old = new WalkerZone(walker.getId(), UUID.randomUUID());
        when(walkerZoneRepository.findAllByWalkerId(walker.getId()))
                .thenReturn(List.of(old), List.of(new WalkerZone(walker.getId(), zoneId)));
        when(zoneService.findAllById(List.of(zoneId)))
                .thenReturn(List.of(new ZoneResponse(zoneId, "Almagro", "Buenos Aires")));

        WalkerCoverageResponse response = service.updateOwnCoverage(walker.getId(),
                new WalkerCoverageRequest(new BigDecimal("3000"), 5, List.of(zoneId)));

        verify(walkerZoneRepository).deleteAll(List.of(old));
        ArgumentCaptor<List<WalkerZone>> captor = ArgumentCaptor.forClass(List.class);
        verify(walkerZoneRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(WalkerZone::getZoneId).containsExactly(zoneId);
        assertThat(walker.getDefaultPrice()).isEqualByComparingTo("3000");
        assertThat(walker.getDefaultCapacity()).isEqualTo(5);
        assertThat(response.zones()).extracting(ZoneResponse::name).containsExactly("Almagro");
    }

    @Test
    void rejectsAnUnknownZoneWithoutTouchingAnything() {
        User walker = walker(UUID.randomUUID(), true);
        UUID unknownZoneId = UUID.randomUUID();
        when(userRepository.findById(walker.getId())).thenReturn(Optional.of(walker));
        when(userRepository.findByIdForUpdate(walker.getId())).thenReturn(Optional.of(walker));
        when(zoneService.existsById(unknownZoneId)).thenReturn(false);

        assertThatThrownBy(() -> service.updateOwnCoverage(walker.getId(),
                new WalkerCoverageRequest(null, null, List.of(unknownZoneId))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Zone not found");
        verify(walkerZoneRepository, never()).deleteAll(anyList());
        verify(walkerZoneRepository, never()).saveAll(anyList());
    }

    @Test
    void rejectsCoverageUpdateFromAClient() {
        User client = client(UUID.randomUUID());
        when(userRepository.findById(client.getId())).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.updateOwnCoverage(client.getId(),
                new WalkerCoverageRequest(null, null, List.of())))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void returnsThePublicProfileOfAnActiveWalkerWithoutContactData() {
        User walker = walker(UUID.randomUUID(), true);
        when(userRepository.findById(walker.getId())).thenReturn(Optional.of(walker));
        when(walkerZoneRepository.findAllByWalkerId(walker.getId())).thenReturn(List.of());
        when(zoneService.findAllById(List.of())).thenReturn(List.of());

        WalkerPublicProfileResponse response = service.getPublicProfile(walker.getId());

        assertThat(response.fullName()).isEqualTo(walker.getFullName());
        assertThat(response.defaultPrice()).isEqualByComparingTo("2500");
    }

    @Test
    void reportsAClientRequestedAsAWalkerAsNotFound() {
        User client = client(UUID.randomUUID());
        when(userRepository.findById(client.getId())).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.getPublicProfile(client.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Walker not found");
    }

    private static User walker(UUID id, boolean active) {
        User user = new User();
        user.setId(id);
        user.setFullName("Paseador de prueba");
        user.setRole(UserRole.WALKER);
        user.setActive(active);
        user.setDefaultPrice(new BigDecimal("2500"));
        user.setDefaultCapacity(4);
        return user;
    }

    private static User client(UUID id) {
        User user = new User();
        user.setId(id);
        user.setFullName("Cliente de prueba");
        user.setRole(UserRole.CLIENT);
        user.setActive(true);
        return user;
    }
}
