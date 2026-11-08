package com.tpi.paseadores.walk.publish;

import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.shared.exception.InvalidFieldException;
import com.tpi.paseadores.user.WalkerCoverageService;
import com.tpi.paseadores.walk.Walk;
import com.tpi.paseadores.walk.WalkRepository;
import com.tpi.paseadores.walk.publish.dto.WalkPublishRequest;
import com.tpi.paseadores.walk.publish.dto.WalkPublishResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class WalkPublicationService {
    private final WalkerCoverageService coverage;
    private final WalkPublicationRepository schedules;
    private final WalkRepository walks;
    private final Clock clock;

    public WalkPublicationService(WalkerCoverageService coverage, WalkPublicationRepository schedules,
                                  WalkRepository walks, Clock clock) {
        this.coverage = coverage;
        this.schedules = schedules;
        this.walks = walks;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalkPublishResponse publish(UUID walkerId, WalkPublishRequest request) {
        schedules.lockSchedule(walkerId);
        var ownCoverage = coverage.getOwnCoverage(walkerId);
        if (ownCoverage.zones().stream().noneMatch(zone -> zone.id().equals(request.zoneId()))) {
            throw new InvalidFieldException("zoneId", "Elegí un barrio dentro de tus zonas de servicio.");
        }
        if (request.startTime().isBefore(LocalDateTime.now(clock).plusHours(2))) {
            throw new InvalidFieldException("startTime", "El paseo debe publicarse con al menos dos horas de anticipación.");
        }
        if (request.durationMinutes() % 15 != 0) {
            throw new InvalidFieldException("durationMinutes", "La duración debe ser múltiplo de 15 minutos.");
        }
        var endTime = request.startTime().plusMinutes(request.durationMinutes());
        if (endTime.getYear() > 9999) {
            throw new InvalidFieldException("startTime", "La fecha del paseo está fuera del rango admitido.");
        }
        if (schedules.hasOverlap(walkerId, request.startTime(), endTime)) {
            throw new BusinessRuleViolationException("Ya tenés un paseo que coincide con ese horario.");
        }

        Walk walk = walks.saveAndFlush(new Walk(walkerId, request.zoneId(), request.startTime(),
                request.durationMinutes(), request.capacity(), request.pricePerPet(), request.meetingPoint().trim()));
        return new WalkPublishResponse(walk.getId());
    }
}
