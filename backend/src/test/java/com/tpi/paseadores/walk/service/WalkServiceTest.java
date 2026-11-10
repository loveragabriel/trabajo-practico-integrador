package com.tpi.paseadores.walk.service;

import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.InvalidFieldException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.user.domain.User;
import com.tpi.paseadores.user.domain.UserRole;
import com.tpi.paseadores.user.service.UserService;
import com.tpi.paseadores.user.service.WalkerCoverageService;
import com.tpi.paseadores.pet.domain.PetSize;
import com.tpi.paseadores.walk.domain.Walk;
import com.tpi.paseadores.walk.domain.WalkStatus;
import com.tpi.paseadores.walk.dto.JoinedPetResponse;
import com.tpi.paseadores.walk.dto.WalkAgendaPeriod;
import com.tpi.paseadores.walk.dto.WalkCreateRequest;
import com.tpi.paseadores.walk.dto.WalkReadResponse;
import com.tpi.paseadores.walk.dto.WalkSearchFilters;
import com.tpi.paseadores.walk.repository.WalkRepository;
import com.tpi.paseadores.walk.repository.WalkSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class WalkServiceTest {
    @Mock WalkRepository walks;
    @Mock WalkPetService walkPets;
    @Mock WalkSearchRepository searchRepository;
    @Mock WalkerCoverageService coverage;
    @Mock UserService users;

    private final UUID walker = UUID.randomUUID();
    private final UUID zone = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private final UUID walkId = UUID.randomUUID();
    private LocalDateTime now;
    private WalkService service;

    @BeforeEach
    void setUp() {
        Clock caba = Clock.fixed(Instant.parse("2030-06-01T13:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));
        now = LocalDateTime.now(caba);
        service = new WalkService(walks, walkPets, searchRepository, coverage, users, caba);
    }

    // --- crear ---------------------------------------------------------------------------------

    @Test
    void createsAtTheTwoHourBoundaryAfterLockingTheScheduleAndCheckingOverlap() {
        LocalDateTime start = now.plusHours(2);
        covered();
        UUID id = UUID.randomUUID();
        when(walks.saveAndFlush(any(Walk.class))).thenAnswer(invocation -> {
            Walk walk = invocation.getArgument(0);
            ReflectionTestUtils.setField(walk, "id", id);
            return walk;
        });
        var created = new WalkReadResponse(id, zone, "Palermo", start, start.plusMinutes(45),
                45, "Entrada de la plaza", new BigDecimal("2500.50"), 3, 0, 3, WalkStatus.PUBLISHED,
                new WalkReadResponse.WalkerSummary(walker, "Paseador", BigDecimal.ZERO, 0));
        when(searchRepository.findVisibleDetail(id)).thenReturn(Optional.of(created));

        assertThat(service.create(walker, request(zone, start, 45)).id()).isEqualTo(id);
        var order = inOrder(walks, coverage);
        order.verify(walks).lockWalkerSchedule("walker-schedule:" + walker);
        order.verify(coverage).coversZone(walker, zone);
        order.verify(walks).hasOverlap(walker, start, start.plusMinutes(45));
        var inserted = ArgumentCaptor.forClass(Walk.class);
        order.verify(walks).saveAndFlush(inserted.capture());
        assertThat(inserted.getValue().getWalkerId()).isEqualTo(walker);
        assertThat(inserted.getValue().getStatus()).isEqualTo(WalkStatus.PUBLISHED);
        assertThat(inserted.getValue().getMeetingPoint()).isEqualTo("Entrada de la plaza");
    }

    @Test
    void rejectsClientsAndInactiveWalkersThroughTheCoverageService() {
        when(coverage.coversZone(walker, zone)).thenThrow(new ForbiddenActionException("Sin permiso"));
        assertThatThrownBy(() -> service.create(walker, request(zone, now.plusHours(2), 45)))
                .isInstanceOf(ForbiddenActionException.class);
        verifyNoInteractions(searchRepository);
        verify(walks, never()).saveAndFlush(any());
    }

    @Test
    void rejectsAZoneOutsideTheWalkersCoverage() {
        // sin covered(): la zona pedida nunca queda cubierta, no hace falta stubear coversZone
        assertThatThrownBy(() -> service.create(walker, request(UUID.randomUUID(), now.plusHours(2), 45)))
                .isInstanceOfSatisfying(InvalidFieldException.class, ex -> assertThat(ex.getField()).isEqualTo("zoneId"));
        verify(walks, never()).saveAndFlush(any());
    }

    @Test
    void rejectsInsufficientNoticeAndInvalidDuration() {
        covered();
        assertThatThrownBy(() -> service.create(walker, request(zone, now.plusHours(2).minusSeconds(1), 45)))
                .isInstanceOfSatisfying(InvalidFieldException.class, ex -> assertThat(ex.getField()).isEqualTo("startTime"));
        assertThatThrownBy(() -> service.create(walker, request(zone, now.plusHours(2), 31)))
                .isInstanceOfSatisfying(InvalidFieldException.class, ex -> assertThat(ex.getField()).isEqualTo("durationMinutes"));
        verify(walks, never()).saveAndFlush(any());
    }

    @Test
    void rejectsAnOverlappingWalkWithoutSaving() {
        LocalDateTime start = now.plusHours(2);
        covered();
        when(walks.hasOverlap(walker, start, start.plusMinutes(45))).thenReturn(true);
        assertThatThrownBy(() -> service.create(walker, request(zone, start, 45)))
                .isInstanceOf(BusinessRuleViolationException.class);
        verify(walks, never()).saveAndFlush(any());
    }

    private void covered() {
        when(coverage.coversZone(walker, zone)).thenReturn(true);
    }

    private WalkCreateRequest request(UUID zoneId, LocalDateTime time, int duration) {
        return new WalkCreateRequest(zoneId, time, duration, 3, new BigDecimal("2500.50"), " Entrada de la plaza ");
    }

    // --- búsqueda -------------------------------------------------------------------------------

    @Test
    void searchUsesCabaTimeAndKeepsOnlyTheRequestedPage() {
        var filters = new WalkSearchFilters(zone, null, null, 0, 1);
        var row = new WalkReadResponse(UUID.randomUUID(), zone, "Palermo", now.plusDays(1), now.plusDays(1).plusHours(1),
                60, "Entrada de la plaza", BigDecimal.TEN, 4, 1, 3, WalkStatus.PUBLISHED,
                new WalkReadResponse.WalkerSummary(UUID.randomUUID(), "Paseador", BigDecimal.ZERO, 0));
        when(searchRepository.findAvailable(filters, now)).thenReturn(List.of(row, row));
        var result = service.search(actorId, filters);
        assertThat(result.items()).containsExactly(row);
        assertThat(result.hasNext()).isTrue();
        verify(searchRepository).findAvailable(filters, now);
    }

    @Test
    void searchWithoutZoneIdDefaultsToTheActorsHomeZone() {
        UUID homeZone = UUID.randomUUID();
        User client = new User();
        client.setId(actorId);
        client.setRole(UserRole.CLIENT);
        client.setActive(true);
        client.setZoneId(homeZone);
        when(users.findById(actorId)).thenReturn(Optional.of(client));
        var withoutZone = new WalkSearchFilters(null, null, null, 0, 12);
        var withHomeZone = new WalkSearchFilters(homeZone, null, null, 0, 12);
        when(searchRepository.findAvailable(withHomeZone, now)).thenReturn(List.of());

        service.search(actorId, withoutZone);

        verify(searchRepository).findAvailable(withHomeZone, now);
    }

    @Test
    void emptySearchHasNoNextPage() {
        var filters = new WalkSearchFilters(zone, null, null, 0, 12);
        when(searchRepository.findAvailable(filters, now)).thenReturn(List.of());
        var result = service.search(actorId, filters);
        assertThat(result.items()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void rejectsEqualAndReversedRangesBeforeQuerying() {
        for (var end : List.of(now, now.minusHours(1))) {
            assertThatThrownBy(() -> service.search(actorId, new WalkSearchFilters(zone, now, end, 0, 12)))
                    .isInstanceOf(InvalidFieldException.class);
        }
        verifyNoInteractions(searchRepository);
    }

    @Test
    void rejectsUnboundedOrNegativeSearchPagination() {
        for (var filters : List.of(new WalkSearchFilters(zone, null, null, -1, 12),
                new WalkSearchFilters(zone, null, null, 0, 0),
                new WalkSearchFilters(zone, null, null, 0, 51))) {
            assertThatThrownBy(() -> service.search(actorId, filters)).isInstanceOf(InvalidFieldException.class);
        }
        verifyNoInteractions(searchRepository);
    }

    // --- agenda (mine) --------------------------------------------------------------------------

    @Test
    void mineUsesAuthenticatedWalkerCabaTimeAndPagination() {
        actor(UserRole.WALKER, true);
        var row = walkDetail(actorId, WalkStatus.PUBLISHED, now.plusDays(1));
        when(searchRepository.findAgenda(actorId, WalkAgendaPeriod.UPCOMING, now, 0, 1)).thenReturn(List.of(row, row));
        var result = service.mine(actorId, WalkAgendaPeriod.UPCOMING, 0, 1);
        assertThat(result.items()).containsExactly(row);
        assertThat(result.hasNext()).isTrue();
        verify(searchRepository).findAgenda(actorId, WalkAgendaPeriod.UPCOMING, now, 0, 1);
    }

    @Test
    void clientsCannotReadWalkerAgenda() {
        actor(UserRole.CLIENT, true);
        assertThatThrownBy(() -> service.mine(actorId, WalkAgendaPeriod.HISTORY, 0, 12))
                .isInstanceOf(ForbiddenActionException.class);
        verifyNoInteractions(searchRepository);
    }

    @Test
    void inactiveOrMissingUsersCannotReadWalks() {
        actor(UserRole.WALKER, false);
        assertThatThrownBy(() -> service.getById(actorId, walkId)).isInstanceOf(ForbiddenActionException.class);
        when(users.findById(actorId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.mine(actorId, WalkAgendaPeriod.UPCOMING, 0, 12))
                .isInstanceOf(ForbiddenActionException.class);
        verifyNoInteractions(searchRepository);
    }

    @Test
    void validatesPaginationBeforeQueryingAgenda() {
        actor(UserRole.WALKER, true);
        for (int[] values : List.of(new int[]{-1, 12}, new int[]{0, 0}, new int[]{0, 51})) {
            assertThatThrownBy(() -> service.mine(actorId, WalkAgendaPeriod.UPCOMING, values[0], values[1]))
                    .isInstanceOf(InvalidFieldException.class);
        }
        verifyNoInteractions(searchRepository);
    }

    // --- detalle (getById) -----------------------------------------------------------------------

    @Test
    void activeClientCanReadPublishedAndFullFutureWalks() {
        actor(UserRole.CLIENT, true);
        for (WalkStatus status : List.of(WalkStatus.PUBLISHED, WalkStatus.FULL)) {
            var row = walkDetail(UUID.randomUUID(), status, now.plusHours(1));
            when(searchRepository.findVisibleDetail(walkId)).thenReturn(Optional.of(row));
            assertThat(service.getById(actorId, walkId)).isEqualTo(row);
        }
        verify(searchRepository, never()).hasActivePet(any(), any());
    }

    @Test
    void ownerCanReadHistoryButAnotherWalkerCannot() {
        actor(UserRole.WALKER, true);
        var row = walkDetail(actorId, WalkStatus.CANCELLED, now.minusDays(1));
        when(searchRepository.findVisibleDetail(walkId)).thenReturn(Optional.of(row));
        assertThat(service.getById(actorId, walkId)).isEqualTo(row);
        when(searchRepository.findVisibleDetail(walkId))
                .thenReturn(Optional.of(walkDetail(UUID.randomUUID(), WalkStatus.COMPLETED, now.minusDays(1))));
        assertThatThrownBy(() -> service.getById(actorId, walkId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void historyRequiresParticipationForClients() {
        actor(UserRole.CLIENT, true);
        var row = walkDetail(UUID.randomUUID(), WalkStatus.COMPLETED, now.minusDays(1));
        when(searchRepository.findVisibleDetail(walkId)).thenReturn(Optional.of(row));
        when(searchRepository.hasActivePet(walkId, actorId)).thenReturn(false);
        assertThatThrownBy(() -> service.getById(actorId, walkId)).isInstanceOf(ResourceNotFoundException.class);
        when(searchRepository.hasActivePet(walkId, actorId)).thenReturn(true);
        assertThat(service.getById(actorId, walkId)).isEqualTo(row);
    }

    @Test
    void missingWalkReturnsNotFound() {
        actor(UserRole.CLIENT, true);
        when(searchRepository.findVisibleDetail(walkId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(actorId, walkId)).isInstanceOf(ResourceNotFoundException.class);
    }

    // --- mascotas inscriptas (getJoinedPets) -----------------------------------------------------

    @Test
    void ownerWalkerSeesTheJoinedPetsWithOwnerContact() {
        Walk walk = walk(WalkStatus.PUBLISHED, 3, now.plusHours(3));
        User owner = new User();
        owner.setId(walker);
        owner.setRole(UserRole.WALKER);
        owner.setActive(true);
        when(users.findById(walker)).thenReturn(Optional.of(owner));
        when(walks.findById(walkId)).thenReturn(Optional.of(walk));
        var pet = new JoinedPetResponse(UUID.randomUUID(), "Rex", "Labrador", PetSize.MEDIUM, "Tira de la correa",
                new JoinedPetResponse.OwnerSummary("Dueño", "+54 11 1234-5678"));
        when(searchRepository.findJoinedPets(walkId)).thenReturn(List.of(pet));

        assertThat(service.getJoinedPets(walker, walkId)).containsExactly(pet);
    }

    @Test
    void onlyTheOwningWalkerCanSeeTheJoinedPets() {
        actor(UserRole.WALKER, true);
        when(walks.findById(walkId)).thenReturn(Optional.of(walk(WalkStatus.PUBLISHED, 3, now.plusHours(3))));
        assertThatThrownBy(() -> service.getJoinedPets(actorId, walkId)).isInstanceOf(ForbiddenActionException.class);
        verifyNoInteractions(searchRepository);
    }

    @Test
    void clientsCannotSeeTheJoinedPets() {
        actor(UserRole.CLIENT, true);
        assertThatThrownBy(() -> service.getJoinedPets(actorId, walkId)).isInstanceOf(ForbiddenActionException.class);
        verify(walks, never()).findById(any());
        verifyNoInteractions(searchRepository);
    }

    @Test
    void missingWalkReturnsNotFoundForJoinedPets() {
        actor(UserRole.WALKER, true);
        when(walks.findById(walkId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getJoinedPets(actorId, walkId)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(searchRepository);
    }

    // --- updateStatus -------------------------------------------------------------------------------
    // join/cancelBooking/myBookings viven en BookingService (ver BookingServiceTest); acá solo
    // queda el ciclo de vida propio del Walk.

    @Test
    void cancellingAWalkCancelsAllJoinedBookingsInTheSameCall() {
        Walk walk = walk(WalkStatus.PUBLISHED, 3, now.plusHours(3));
        walkOwner(walk);

        service.updateStatus(walker, walkId, WalkStatus.CANCELLED);

        verify(walkPets).cancelAllJoined(walkId);
        assertThat(walk.getStatus()).isEqualTo(WalkStatus.CANCELLED);
    }

    @Test
    void rejectsStartingAWalkWithNoBookings() {
        Walk walk = walk(WalkStatus.PUBLISHED, 3, now.plusHours(3));
        walkOwner(walk);
        when(walkPets.hasAnyJoined(walkId)).thenReturn(false);
        assertThatThrownBy(() -> service.updateStatus(walker, walkId, WalkStatus.IN_PROGRESS))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThat(walk.getStatus()).isEqualTo(WalkStatus.PUBLISHED);
    }

    @Test
    void cantCompleteAWalkThatNeverStarted() {
        // RN-29: COMPLETED solo puede venir de IN_PROGRESS. Esto es justo lo que la rama de
        // referencia no validaba (dejaba completar un PUBLISHED/FULL directamente).
        Walk walk = walk(WalkStatus.PUBLISHED, 3, now.plusHours(3));
        walkOwner(walk);
        assertThatThrownBy(() -> service.updateStatus(walker, walkId, WalkStatus.COMPLETED))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThat(walk.getStatus()).isEqualTo(WalkStatus.PUBLISHED);
    }

    @Test
    void completesAWalkThatIsInProgress() {
        Walk walk = walk(WalkStatus.IN_PROGRESS, 3, now.plusHours(3));
        walkOwner(walk);
        service.updateStatus(walker, walkId, WalkStatus.COMPLETED);
        assertThat(walk.getStatus()).isEqualTo(WalkStatus.COMPLETED);
    }

    @Test
    void rejectsForcingTheAutomaticStatuses() {
        for (WalkStatus target : List.of(WalkStatus.PUBLISHED, WalkStatus.FULL)) {
            assertThatThrownBy(() -> service.updateStatus(walker, walkId, target))
                    .isInstanceOf(InvalidFieldException.class);
        }
        verifyNoInteractions(walks);
    }

    @Test
    void onlyTheOwningWalkerCanChangeStatus() {
        actor(UserRole.WALKER, true);
        when(walks.findByIdForUpdate(walkId)).thenReturn(Optional.of(walk(WalkStatus.PUBLISHED, 3, now.plusHours(3))));
        assertThatThrownBy(() -> service.updateStatus(actorId, walkId, WalkStatus.CANCELLED))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void clientsCannotChangeWalkStatus() {
        actor(UserRole.CLIENT, true);
        assertThatThrownBy(() -> service.updateStatus(actorId, walkId, WalkStatus.CANCELLED))
                .isInstanceOf(ForbiddenActionException.class);
        verifyNoInteractions(walks);
    }

    private void walkOwner(Walk walk) {
        User user = new User();
        user.setId(walker);
        user.setRole(UserRole.WALKER);
        user.setActive(true);
        when(users.findById(walker)).thenReturn(Optional.of(user));
        when(walks.findByIdForUpdate(walkId)).thenReturn(Optional.of(walk));
    }

    private Walk walk(WalkStatus status, int capacity, LocalDateTime startTime) {
        Walk walk = new Walk(walker, zone, startTime, 60, capacity, BigDecimal.TEN, "Entrada de la plaza");
        ReflectionTestUtils.setField(walk, "id", walkId);
        ReflectionTestUtils.setField(walk, "status", status);
        return walk;
    }

    private void actor(UserRole role, boolean active) {
        User user = new User();
        user.setId(actorId);
        user.setRole(role);
        user.setActive(active);
        when(users.findById(actorId)).thenReturn(Optional.of(user));
    }

    private WalkReadResponse walkDetail(UUID walkerId, WalkStatus status, LocalDateTime start) {
        return new WalkReadResponse(walkId, UUID.randomUUID(), "Palermo", start, start.plusHours(1),
                60, "Entrada de la plaza", BigDecimal.TEN, 3, 1, 2, status,
                new WalkReadResponse.WalkerSummary(walkerId, "Paseador", BigDecimal.ZERO, 0));
    }
}
