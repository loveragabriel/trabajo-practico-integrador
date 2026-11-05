package com.tpi.paseadores.walk.query;

import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.InvalidFieldException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.user.User;
import com.tpi.paseadores.user.UserRole;
import com.tpi.paseadores.user.UserService;
import com.tpi.paseadores.walk.WalkStatus;
import com.tpi.paseadores.walk.query.dto.WalkAgendaPeriod;
import com.tpi.paseadores.walk.query.dto.WalkReadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

class WalkReadServiceTest {
    private final WalkReadRepository repository = mock(WalkReadRepository.class);
    private final UserService users = mock(UserService.class);
    private final UUID actorId = UUID.randomUUID();
    private final UUID walkId = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.of(2026, 11, 5, 10, 0);
    private WalkReadService service;

    @BeforeEach
    void setup() {
        service = new WalkReadService(repository, users, Clock.fixed(Instant.parse("2026-11-05T13:00:00Z"),
                ZoneId.of("America/Argentina/Buenos_Aires")));
        actor(UserRole.WALKER, true);
    }

    @Test
    void agendaUsesAuthenticatedWalkerCabaTimeAndPagination() {
        var row = walk(actorId, WalkStatus.PUBLISHED, now.plusDays(1));
        when(repository.findAgenda(actorId, WalkAgendaPeriod.UPCOMING, now, 0, 1)).thenReturn(List.of(row, row));
        var result = service.agenda(actorId, WalkAgendaPeriod.UPCOMING, 0, 1);
        assertThat(result.items()).containsExactly(row);
        assertThat(result.hasNext()).isTrue();
        verify(repository).findAgenda(actorId, WalkAgendaPeriod.UPCOMING, now, 0, 1);
    }

    @Test
    void clientsCannotReadWalkerAgenda() {
        actor(UserRole.CLIENT, true);
        assertThatThrownBy(() -> service.agenda(actorId, WalkAgendaPeriod.HISTORY, 0, 12))
                .isInstanceOf(ForbiddenActionException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void inactiveOrMissingUsersCannotReadWalks() {
        actor(UserRole.WALKER, false);
        assertThatThrownBy(() -> service.detail(actorId, walkId)).isInstanceOf(ForbiddenActionException.class);
        when(users.findById(actorId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.agenda(actorId, WalkAgendaPeriod.UPCOMING, 0, 12))
                .isInstanceOf(ForbiddenActionException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void validatesPaginationBeforeQueryingAgenda() {
        for (int[] values : List.of(new int[]{-1, 12}, new int[]{0, 0}, new int[]{0, 51})) {
            assertThatThrownBy(() -> service.agenda(actorId, WalkAgendaPeriod.UPCOMING, values[0], values[1]))
                    .isInstanceOf(InvalidFieldException.class);
        }
        verifyNoInteractions(repository);
    }

    @Test
    void activeClientCanReadPublishedAndFullFutureWalks() {
        actor(UserRole.CLIENT, true);
        for (WalkStatus status : List.of(WalkStatus.PUBLISHED, WalkStatus.FULL)) {
            var row = walk(UUID.randomUUID(), status, now.plusHours(1));
            when(repository.findVisibleDetail(walkId)).thenReturn(Optional.of(row));
            assertThat(service.detail(actorId, walkId)).isEqualTo(row);
        }
        verify(repository, never()).hasJoinedBooking(any(), any());
    }

    @Test
    void ownerCanReadHistoryButAnotherWalkerCannot() {
        var row = walk(actorId, WalkStatus.CANCELLED, now.minusDays(1));
        when(repository.findVisibleDetail(walkId)).thenReturn(Optional.of(row));
        assertThat(service.detail(actorId, walkId)).isEqualTo(row);
        when(repository.findVisibleDetail(walkId)).thenReturn(Optional.of(walk(UUID.randomUUID(), WalkStatus.COMPLETED, now.minusDays(1))));
        assertThatThrownBy(() -> service.detail(actorId, walkId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void historyRequiresParticipationForClients() {
        actor(UserRole.CLIENT, true);
        var row = walk(UUID.randomUUID(), WalkStatus.COMPLETED, now.minusDays(1));
        when(repository.findVisibleDetail(walkId)).thenReturn(Optional.of(row));
        when(repository.hasJoinedBooking(walkId, actorId)).thenReturn(false);
        assertThatThrownBy(() -> service.detail(actorId, walkId)).isInstanceOf(ResourceNotFoundException.class);
        when(repository.hasJoinedBooking(walkId, actorId)).thenReturn(true);
        assertThat(service.detail(actorId, walkId)).isEqualTo(row);
    }

    @Test
    void missingWalkReturnsNotFound() {
        when(repository.findVisibleDetail(walkId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.detail(actorId, walkId)).isInstanceOf(ResourceNotFoundException.class);
    }

    private void actor(UserRole role, boolean active) {
        User user = new User();
        user.setId(actorId); user.setRole(role); user.setActive(active);
        when(users.findById(actorId)).thenReturn(Optional.of(user));
    }

    private WalkReadResponse walk(UUID walker, WalkStatus status, LocalDateTime start) {
        return new WalkReadResponse(walkId, UUID.randomUUID(), "Palermo", start, start.plusHours(1),
                60, "Entrada de la plaza", BigDecimal.TEN, 3, 1, 2, status,
                new WalkReadResponse.WalkerSummary(walker, "Paseador", BigDecimal.ZERO, 0));
    }
}
