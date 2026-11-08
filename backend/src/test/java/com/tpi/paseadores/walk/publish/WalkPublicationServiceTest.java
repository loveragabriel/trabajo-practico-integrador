package com.tpi.paseadores.walk.publish;

import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.InvalidFieldException;
import com.tpi.paseadores.user.WalkerCoverageService;
import com.tpi.paseadores.user.dto.WalkerCoverageResponse;
import com.tpi.paseadores.walk.Walk;
import com.tpi.paseadores.walk.WalkRepository;
import com.tpi.paseadores.walk.WalkStatus;
import com.tpi.paseadores.walk.publish.dto.WalkPublishRequest;
import com.tpi.paseadores.zone.ZoneResponse;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalkPublicationServiceTest {
    @Mock WalkerCoverageService coverage;
    @Mock WalkPublicationRepository schedules;
    @Mock WalkRepository walks;

    private final UUID walker = UUID.randomUUID();
    private final UUID zone = UUID.randomUUID();
    private final LocalDateTime start = LocalDateTime.of(2030, 1, 1, 11, 0);
    private WalkPublicationService service;

    @BeforeEach
    void setUp() {
        Clock caba = Clock.fixed(Instant.parse("2030-01-01T12:00:00Z"),
                ZoneId.of("America/Argentina/Buenos_Aires"));
        service = new WalkPublicationService(coverage, schedules, walks, caba);
    }

    @Test
    void publishesAtTheTwoHourBoundaryInCabaAfterLockingTheSchedule() {
        covered();
        UUID id = UUID.randomUUID();
        when(walks.saveAndFlush(any(Walk.class))).thenAnswer(invocation -> {
            Walk walk = invocation.getArgument(0);
            ReflectionTestUtils.setField(walk, "id", id);
            return walk;
        });

        assertThat(service.publish(walker, request(zone, start, 45)).id()).isEqualTo(id);
        var order = inOrder(schedules, coverage, walks);
        order.verify(schedules).lockSchedule(walker);
        order.verify(coverage).getOwnCoverage(walker);
        order.verify(schedules).hasOverlap(walker, start, start.plusMinutes(45));
        var inserted = ArgumentCaptor.forClass(Walk.class);
        order.verify(walks).saveAndFlush(inserted.capture());
        assertThat(inserted.getValue().getWalkerId()).isEqualTo(walker);
        assertThat(inserted.getValue().getStatus()).isEqualTo(WalkStatus.PUBLISHED);
        assertThat(inserted.getValue().getMeetingPoint()).isEqualTo("Entrada de la plaza");
    }

    @Test
    void rejectsClientsAndInactiveWalkersThroughTheCoverageService() {
        when(coverage.getOwnCoverage(walker)).thenThrow(new ForbiddenActionException("Sin permiso"));
        assertThatThrownBy(() -> service.publish(walker, request(zone, start, 45)))
                .isInstanceOf(ForbiddenActionException.class);
        verifyNoInteractions(walks);
    }

    @Test
    void rejectsAZoneOutsideTheWalkersCoverage() {
        covered();
        assertThatThrownBy(() -> service.publish(walker, request(UUID.randomUUID(), start, 45)))
                .isInstanceOfSatisfying(InvalidFieldException.class, ex -> assertThat(ex.getField()).isEqualTo("zoneId"));
        verifyNoInteractions(walks);
    }

    @Test
    void rejectsInsufficientNoticeAndInvalidDuration() {
        covered();
        assertThatThrownBy(() -> service.publish(walker, request(zone, start.minusSeconds(1), 45)))
                .isInstanceOfSatisfying(InvalidFieldException.class, ex -> assertThat(ex.getField()).isEqualTo("startTime"));
        assertThatThrownBy(() -> service.publish(walker, request(zone, start, 31)))
                .isInstanceOfSatisfying(InvalidFieldException.class, ex -> assertThat(ex.getField()).isEqualTo("durationMinutes"));
        verifyNoInteractions(walks);
    }

    @Test
    void rejectsAnOverlappingWalkWithoutSaving() {
        covered();
        when(schedules.hasOverlap(walker, start, start.plusMinutes(45))).thenReturn(true);
        assertThatThrownBy(() -> service.publish(walker, request(zone, start, 45)))
                .isInstanceOf(BusinessRuleViolationException.class);
        verifyNoInteractions(walks);
    }

    private void covered() {
        when(coverage.getOwnCoverage(walker)).thenReturn(new WalkerCoverageResponse(null, null,
                List.of(new ZoneResponse(zone, "Palermo", "Buenos Aires"))));
    }

    private WalkPublishRequest request(UUID zoneId, LocalDateTime time, int duration) {
        return new WalkPublishRequest(zoneId, time, duration, 3, new BigDecimal("2500.50"), " Entrada de la plaza ");
    }
}
