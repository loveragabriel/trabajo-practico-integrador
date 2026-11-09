package com.tpi.paseadores.walk.service;

import com.tpi.paseadores.pet.domain.Pet;
import com.tpi.paseadores.pet.service.PetService;
import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.InvalidFieldException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.user.domain.User;
import com.tpi.paseadores.user.domain.UserRole;
import com.tpi.paseadores.user.service.UserService;
import com.tpi.paseadores.walk.domain.BookingStatus;
import com.tpi.paseadores.walk.domain.Walk;
import com.tpi.paseadores.walk.domain.WalkStatus;
import com.tpi.paseadores.walk.dto.BookingResponse;
import com.tpi.paseadores.walk.dto.ClientBookingResponse;
import com.tpi.paseadores.walk.dto.WalkAgendaPeriod;
import com.tpi.paseadores.walk.repository.WalkRepository;
import com.tpi.paseadores.walk.repository.WalkSearchRepository;
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
class BookingServiceTest {
    @Mock WalkRepository walks;
    @Mock WalkPetService walkPets;
    @Mock WalkSearchRepository searchRepository;
    @Mock UserService users;
    @Mock PetService pets;

    private final UUID walker = UUID.randomUUID();
    private final UUID zone = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private final UUID walkId = UUID.randomUUID();
    private final UUID petId = UUID.randomUUID();
    private LocalDateTime now;
    private BookingService service;

    @BeforeEach
    void setUp() {
        Clock caba = Clock.fixed(Instant.parse("2030-06-01T13:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));
        now = LocalDateTime.now(caba);
        service = new BookingService(walks, walkPets, searchRepository, users, pets, caba);
    }

    // --- join: orquestación — las reglas de la reserva se prueban en WalkPetServiceTest --------

    @Test
    void joinLocksThePetAndTheWalkThenDelegatesToWalkPetService() {
        actor(UserRole.CLIENT, true);
        Pet pet = pet();
        when(pets.getPetEntity(actorId, petId)).thenReturn(pet);
        Walk walk = walk(WalkStatus.PUBLISHED, 1, now.plusHours(3));
        when(walks.findByIdForUpdate(walkId)).thenReturn(Optional.of(walk));
        var expected = new BookingResponse(UUID.randomUUID(), petId, "Rex", BookingStatus.JOINED, now);
        when(walkPets.join(walk, pet)).thenReturn(expected);

        assertThat(service.join(actorId, walkId, petId)).isEqualTo(expected);
    }

    @Test
    void joinPropagatesAMissingWalk() {
        actor(UserRole.CLIENT, true);
        when(pets.getPetEntity(actorId, petId)).thenReturn(pet());
        when(walks.findByIdForUpdate(walkId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.join(actorId, walkId, petId))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(walkPets);
    }

    @Test
    void onlyClientsCanJoinAWalk() {
        actor(UserRole.WALKER, true);
        assertThatThrownBy(() -> service.join(actorId, walkId, petId))
                .isInstanceOf(ForbiddenActionException.class);
        verifyNoInteractions(pets, walks, walkPets);
    }

    // --- cancelBooking: orquestación -----------------------------------------------------------

    @Test
    void cancelBookingLocksThePetAndTheWalkThenDelegatesToWalkPetService() {
        actor(UserRole.CLIENT, true);
        when(pets.getPetEntity(actorId, petId)).thenReturn(pet());
        Walk walk = walk(WalkStatus.FULL, 1, now.plusHours(3));
        when(walks.findByIdForUpdate(walkId)).thenReturn(Optional.of(walk));

        service.cancelBooking(actorId, walkId, petId);

        verify(walkPets).cancel(walk, petId);
    }

    @Test
    void cancelBookingPropagatesAMissingWalk() {
        actor(UserRole.CLIENT, true);
        when(pets.getPetEntity(actorId, petId)).thenReturn(pet());
        when(walks.findByIdForUpdate(walkId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancelBooking(actorId, walkId, petId))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(walkPets);
    }

    @Test
    void onlyClientsCanCancelABooking() {
        actor(UserRole.WALKER, true);
        assertThatThrownBy(() -> service.cancelBooking(actorId, walkId, petId))
                .isInstanceOf(ForbiddenActionException.class);
        verifyNoInteractions(pets, walks, walkPets);
    }

    // --- myBookings -------------------------------------------------------------------------------

    @Test
    void myBookingsUsesCabaTimeAndKeepsOnlyTheRequestedPage() {
        actor(UserRole.CLIENT, true);
        var row = clientBooking();
        when(searchRepository.findBookingsForClient(actorId, WalkAgendaPeriod.UPCOMING, now, 0, 1))
                .thenReturn(List.of(row, row));
        var result = service.myBookings(actorId, WalkAgendaPeriod.UPCOMING, 0, 1);
        assertThat(result.items()).containsExactly(row);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void onlyClientsCanListTheirBookings() {
        actor(UserRole.WALKER, true);
        assertThatThrownBy(() -> service.myBookings(actorId, WalkAgendaPeriod.UPCOMING, 0, 12))
                .isInstanceOf(ForbiddenActionException.class);
        verifyNoInteractions(searchRepository);
    }

    @Test
    void inactiveOrMissingUsersCannotListBookings() {
        when(users.findById(actorId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.myBookings(actorId, WalkAgendaPeriod.UPCOMING, 0, 12))
                .isInstanceOf(ForbiddenActionException.class);
        verifyNoInteractions(searchRepository);
    }

    @Test
    void validatesPaginationBeforeQueryingBookings() {
        actor(UserRole.CLIENT, true);
        for (int[] values : List.of(new int[]{-1, 12}, new int[]{0, 0}, new int[]{0, 51})) {
            assertThatThrownBy(() -> service.myBookings(actorId, WalkAgendaPeriod.UPCOMING, values[0], values[1]))
                    .isInstanceOf(InvalidFieldException.class);
        }
        verifyNoInteractions(searchRepository);
    }

    private void actor(UserRole role, boolean active) {
        User user = new User();
        user.setId(actorId);
        user.setRole(role);
        user.setActive(active);
        when(users.findById(actorId)).thenReturn(Optional.of(user));
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

    private ClientBookingResponse clientBooking() {
        return new ClientBookingResponse(UUID.randomUUID(), BookingStatus.JOINED, petId,
                "Rex", walkId, WalkStatus.PUBLISHED, now.plusDays(1), now.plusDays(1).plusHours(1),
                "Entrada de la plaza", BigDecimal.TEN, "Palermo",
                new ClientBookingResponse.WalkerSummary(walker, "Paseador", BigDecimal.ZERO, 0));
    }
}
