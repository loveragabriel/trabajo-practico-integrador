package com.tpi.paseadores.user.service;

import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.user.domain.User;
import com.tpi.paseadores.user.domain.UserRole;
import com.tpi.paseadores.user.dto.UserResponse;
import com.tpi.paseadores.user.dto.UserUpdateRequest;
import com.tpi.paseadores.user.repository.UserRepository;
import com.tpi.paseadores.zone.service.ZoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private ZoneService zoneService;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, zoneService);
    }

    @Test
    void returnsTheOwnProfile() {
        User user = user(UUID.randomUUID(), "Firulais", UUID.randomUUID());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UserResponse response = service.getProfile(user.getId());

        assertThat(response.fullName()).isEqualTo("Firulais");
        assertThat(response.zoneId()).isEqualTo(user.getZoneId());
    }

    @Test
    void reportsAnUnknownUserAsNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void updatesContactDataAndZoneOfAnExistingUser() {
        UUID newZoneId = UUID.randomUUID();
        User user = user(UUID.randomUUID(), "Firulais", UUID.randomUUID());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(zoneService.existsById(newZoneId)).thenReturn(true);

        UserResponse response = service.updateProfile(user.getId(),
                new UserUpdateRequest("  Gabriel Lovera ", " ", newZoneId));

        assertThat(response.fullName()).isEqualTo("Gabriel Lovera");
        assertThat(response.phone()).isNull();
        assertThat(response.zoneId()).isEqualTo(newZoneId);
        assertThat(user.getZoneId()).isEqualTo(newZoneId);
    }

    @Test
    void rejectsAnUnknownZone() {
        User user = user(UUID.randomUUID(), "Firulais", UUID.randomUUID());
        UUID unknownZoneId = UUID.randomUUID();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(zoneService.existsById(unknownZoneId)).thenReturn(false);

        assertThatThrownBy(() -> service.updateProfile(user.getId(),
                new UserUpdateRequest("Gabriel", null, unknownZoneId)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Zone not found");
    }

    @Test
    void rejectsUpdatingAnUnknownUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProfile(userId,
                new UserUpdateRequest("Gabriel", null, UUID.randomUUID())))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void theFirstRatingSetsTheAverageToThatRating() {
        User walker = user(UUID.randomUUID(), "Paseador", UUID.randomUUID());
        when(userRepository.findByIdForUpdate(walker.getId())).thenReturn(Optional.of(walker));

        service.applyReviewRating(walker.getId(), 4);

        assertThat(walker.getRatingAvg()).isEqualByComparingTo("4.0");
        assertThat(walker.getReviewCount()).isEqualTo(1);
    }

    @Test
    void laterRatingsAverageWithThePreviousOnesAndRoundToOneDecimal() {
        User walker = user(UUID.randomUUID(), "Paseador", UUID.randomUUID());
        walker.setRatingAvg(new BigDecimal("4.0"));
        walker.setReviewCount(2);
        when(userRepository.findByIdForUpdate(walker.getId())).thenReturn(Optional.of(walker));

        // (4.0*2 + 5) / 3 = 4.333... -> redondea a 4.3
        service.applyReviewRating(walker.getId(), 5);

        assertThat(walker.getRatingAvg()).isEqualByComparingTo("4.3");
        assertThat(walker.getReviewCount()).isEqualTo(3);
    }

    @Test
    void rejectsRatingAnUnknownWalker() {
        UUID walkerId = UUID.randomUUID();
        when(userRepository.findByIdForUpdate(walkerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.applyReviewRating(walkerId, 5))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Walker not found");
    }

    private static User user(UUID id, String fullName, UUID zoneId) {
        User user = new User();
        user.setId(id);
        user.setFullName(fullName);
        user.setEmail("firulais@example.com");
        user.setRole(UserRole.CLIENT);
        user.setZoneId(zoneId);
        user.setActive(true);
        return user;
    }
}
