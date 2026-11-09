package com.tpi.paseadores.walk.service;

import com.tpi.paseadores.pet.domain.Pet;
import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.walk.domain.BookingStatus;
import com.tpi.paseadores.walk.domain.Walk;
import com.tpi.paseadores.walk.domain.WalkPet;
import com.tpi.paseadores.walk.domain.WalkStatus;
import com.tpi.paseadores.walk.repository.WalkPetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalkPetServiceTest {
    @Mock WalkPetRepository walkPetRepository;

    private final UUID walker = UUID.randomUUID();
    private final UUID zone = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private final UUID walkId = UUID.randomUUID();
    private final UUID petId = UUID.randomUUID();
    private LocalDateTime now;
    private WalkPetService service;

    @BeforeEach
    void setUp() {
        Clock caba = Clock.fixed(Instant.parse("2030-06-01T13:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));
        now = LocalDateTime.now(caba);
        service = new WalkPetService(walkPetRepository, caba);
    }

    // --- join -----------------------------------------------------------------------------------

    @Test
    void joinReducesAvailableSpotsAndCanFlipAWalkToFull() {
        Pet pet = pet();
        Walk walk = walk(WalkStatus.PUBLISHED, 1, now.plusHours(3));
        when(walkPetRepository.countByWalkIdAndStatus(walkId, BookingStatus.JOINED)).thenReturn(0L);
        when(walkPetRepository.findByWalkIdAndPetId(walkId, petId)).thenReturn(Optional.empty());
        when(walkPetRepository.save(any(WalkPet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.join(walk, pet);

        assertThat(response.petName()).isEqualTo("Rex");
        assertThat(response.status()).isEqualTo(BookingStatus.JOINED);
        assertThat(walk.getStatus()).isEqualTo(WalkStatus.FULL);
    }

    @Test
    void rejectsJoinOnAWalkThatIsNotPublished() {
        Walk walk = walk(WalkStatus.FULL, 1, now.plusHours(3));
        assertThatThrownBy(() -> service.join(walk, pet()))
                .isInstanceOf(BusinessRuleViolationException.class);
        verify(walkPetRepository, never()).save(any());
    }

    @Test
    void rejectsJoinWhenCapacityIsAlreadyReached() {
        Walk walk = walk(WalkStatus.PUBLISHED, 1, now.plusHours(3));
        when(walkPetRepository.countByWalkIdAndStatus(walkId, BookingStatus.JOINED)).thenReturn(1L);
        assertThatThrownBy(() -> service.join(walk, pet()))
                .isInstanceOf(BusinessRuleViolationException.class);
        verify(walkPetRepository, never()).save(any());
    }

    @Test
    void rejectsJoiningTwiceWithTheSamePet() {
        Walk walk = walk(WalkStatus.PUBLISHED, 3, now.plusHours(3));
        when(walkPetRepository.countByWalkIdAndStatus(walkId, BookingStatus.JOINED)).thenReturn(1L);
        when(walkPetRepository.findByWalkIdAndPetId(walkId, petId)).thenReturn(Optional.of(booking(BookingStatus.JOINED)));
        assertThatThrownBy(() -> service.join(walk, pet()))
                .isInstanceOf(BusinessRuleViolationException.class);
        verify(walkPetRepository, never()).save(any());
    }

    @Test
    void revivesACancelledBookingInsteadOfInsertingANewRow() {
        Walk walk = walk(WalkStatus.PUBLISHED, 3, now.plusHours(3));
        when(walkPetRepository.countByWalkIdAndStatus(walkId, BookingStatus.JOINED)).thenReturn(0L);
        WalkPet cancelled = booking(BookingStatus.CANCELLED);
        when(walkPetRepository.findByWalkIdAndPetId(walkId, petId)).thenReturn(Optional.of(cancelled));

        var response = service.join(walk, pet());

        assertThat(response.id()).isEqualTo(cancelled.getId());
        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.JOINED);
        verify(walkPetRepository, never()).save(any());
    }

    // --- cancel ---------------------------------------------------------------------------------

    @Test
    void cancelFreesASpotAndCanReopenAFullWalk() {
        Walk walk = walk(WalkStatus.FULL, 1, now.plusHours(3));
        WalkPet joined = booking(BookingStatus.JOINED);
        when(walkPetRepository.findByWalkIdAndPetId(walkId, petId)).thenReturn(Optional.of(joined));
        when(walkPetRepository.countByWalkIdAndStatus(walkId, BookingStatus.JOINED)).thenReturn(1L);

        service.cancel(walk, petId);

        assertThat(joined.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(walk.getStatus()).isEqualTo(WalkStatus.PUBLISHED);
    }

    @Test
    void rejectsCancellationWithinTwoHoursOfStart() {
        Walk walk = walk(WalkStatus.FULL, 1, now.plusHours(1));
        when(walkPetRepository.findByWalkIdAndPetId(walkId, petId)).thenReturn(Optional.of(booking(BookingStatus.JOINED)));
        assertThatThrownBy(() -> service.cancel(walk, petId))
                .isInstanceOf(BusinessRuleViolationException.class);
        verify(walkPetRepository, never()).countByWalkIdAndStatus(any(), any());
    }

    @Test
    void cancellingAnAlreadyCancelledBookingIsANoOp() {
        Walk walk = walk(WalkStatus.FULL, 1, now.plusHours(3));
        when(walkPetRepository.findByWalkIdAndPetId(walkId, petId)).thenReturn(Optional.of(booking(BookingStatus.CANCELLED)));
        service.cancel(walk, petId);
        verify(walkPetRepository, never()).countByWalkIdAndStatus(any(), any());
    }

    @Test
    void rejectsCancellingAMissingBooking() {
        Walk walk = walk(WalkStatus.FULL, 1, now.plusHours(3));
        when(walkPetRepository.findByWalkIdAndPetId(walkId, petId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancel(walk, petId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- cancelAllJoined / hasAnyJoined (usados por WalkService.updateStatus) ------------------

    @Test
    void cancelAllJoinedCancelsEveryJoinedBookingOfTheWalk() {
        WalkPet first = booking(BookingStatus.JOINED);
        WalkPet second = booking(BookingStatus.JOINED);
        when(walkPetRepository.findAllByWalkIdAndStatus(walkId, BookingStatus.JOINED)).thenReturn(List.of(first, second));

        service.cancelAllJoined(walkId);

        assertThat(first.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(second.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void hasAnyJoinedReflectsTheJoinedCount() {
        when(walkPetRepository.countByWalkIdAndStatus(walkId, BookingStatus.JOINED)).thenReturn(0L);
        assertThat(service.hasAnyJoined(walkId)).isFalse();
        when(walkPetRepository.countByWalkIdAndStatus(walkId, BookingStatus.JOINED)).thenReturn(1L);
        assertThat(service.hasAnyJoined(walkId)).isTrue();
    }

    private Walk walk(WalkStatus status, int capacity, LocalDateTime startTime) {
        Walk walk = new Walk(walker, zone, startTime, 60, capacity, BigDecimal.TEN, "Entrada de la plaza");
        ReflectionTestUtils.setField(walk, "id", walkId);
        ReflectionTestUtils.setField(walk, "status", status);
        return walk;
    }

    private Pet pet() {
        Pet pet = new Pet(actorId);
        pet.setId(petId);
        pet.setName("Rex");
        return pet;
    }

    private WalkPet booking(BookingStatus status) {
        WalkPet booking = new WalkPet(walkId, petId, now);
        ReflectionTestUtils.setField(booking, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(booking, "status", status);
        return booking;
    }
}
