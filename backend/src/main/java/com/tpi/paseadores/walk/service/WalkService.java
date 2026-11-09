package com.tpi.paseadores.walk.service;

import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.InvalidFieldException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.user.domain.User;
import com.tpi.paseadores.user.domain.UserRole;
import com.tpi.paseadores.user.service.UserService;
import com.tpi.paseadores.user.service.WalkerCoverageService;
import com.tpi.paseadores.walk.domain.Walk;
import com.tpi.paseadores.walk.domain.WalkStatus;
import com.tpi.paseadores.walk.dto.WalkAgendaPeriod;
import com.tpi.paseadores.walk.dto.WalkAgendaResponse;
import com.tpi.paseadores.walk.dto.WalkCreateRequest;
import com.tpi.paseadores.walk.dto.WalkReadResponse;
import com.tpi.paseadores.walk.dto.WalkResponse;
import com.tpi.paseadores.walk.dto.WalkSearchFilters;
import com.tpi.paseadores.walk.dto.WalkSearchPageResponse;
import com.tpi.paseadores.walk.repository.WalkRepository;
import com.tpi.paseadores.walk.repository.WalkSearchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WalkService {
    private final WalkRepository walkRepository;
    private final WalkSearchRepository walkSearchRepository;
    private final WalkerCoverageService coverage;
    private final UserService users;
    private final Clock clock;

    public WalkService(WalkRepository walkRepository, WalkSearchRepository walkSearchRepository,
                        WalkerCoverageService coverage, UserService users, Clock clock) {
        this.walkRepository = walkRepository;
        this.walkSearchRepository = walkSearchRepository;
        this.coverage = coverage;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalkResponse create(UUID walkerId, WalkCreateRequest request) {
        walkRepository.lockWalkerSchedule("walker-schedule:" + walkerId);
        if (!coverage.coversZone(walkerId, request.zoneId())) {
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
        if (walkRepository.hasOverlap(walkerId, request.startTime(), endTime)) {
            throw new BusinessRuleViolationException("Ya tenés un paseo que coincide con ese horario.");
        }

        Walk walk = walkRepository.saveAndFlush(new Walk(walkerId, request.zoneId(), request.startTime(),
                request.durationMinutes(), request.capacity(), request.pricePerPet(), request.meetingPoint().trim()));
        return new WalkResponse(walk.getId());
    }

    @Transactional(readOnly = true)
    public WalkSearchPageResponse search(UUID actorId, WalkSearchFilters filters) {
        if (filters.page() < 0) throw new InvalidFieldException("page", "La página no puede ser negativa.");
        if (filters.size() < 1 || filters.size() > 50) {
            throw new InvalidFieldException("size", "El tamaño de página debe estar entre 1 y 50.");
        }
        if (filters.startsFrom() != null && filters.startsTo() != null
                && !filters.startsTo().isAfter(filters.startsFrom())) {
            throw new InvalidFieldException("startsTo", "Hasta debe ser posterior a Desde.");
        }
        // sin zoneId, se busca en la zona del usuario autenticado
        if (filters.zoneId() == null) {
            User actor = activeUser(actorId);
            filters = new WalkSearchFilters(actor.getZoneId(), filters.startsFrom(), filters.startsTo(),
                    filters.page(), filters.size());
        }
        // the repository returns up to size + 1 rows: the extra one only tells whether a next page exists
        var rows = walkSearchRepository.findAvailable(filters, LocalDateTime.now(clock));
        boolean hasNext = rows.size() > filters.size();
        var items = List.copyOf(rows.subList(0, Math.min(filters.size(), rows.size())));
        return new WalkSearchPageResponse(items, filters.page(), filters.size(), hasNext);
    }

    @Transactional(readOnly = true)
    public WalkAgendaResponse mine(UUID actorId, WalkAgendaPeriod period, int page, int size) {
        User actor = activeUser(actorId);
        if (actor.getRole() != UserRole.WALKER) {
            throw new ForbiddenActionException("La agenda está disponible para paseadores activos.");
        }
        if (page < 0) throw new InvalidFieldException("page", "La página no puede ser negativa.");
        if (size < 1 || size > 50) throw new InvalidFieldException("size", "El tamaño de página debe estar entre 1 y 50.");
        var rows = walkSearchRepository.findAgenda(actorId, period, LocalDateTime.now(clock), page, size);
        return new WalkAgendaResponse(List.copyOf(rows.subList(0, Math.min(rows.size(), size))),
                page, size, rows.size() > size);
    }

    @Transactional(readOnly = true)
    public WalkReadResponse getById(UUID actorId, UUID walkId) {
        User actor = activeUser(actorId);
        var walk = walkSearchRepository.findVisibleDetail(walkId)
                .orElseThrow(() -> new ResourceNotFoundException("Paseo no encontrado."));
        boolean offered = (walk.status() == WalkStatus.PUBLISHED || walk.status() == WalkStatus.FULL)
                && walk.startTime().isAfter(LocalDateTime.now(clock));
        boolean own = actor.getRole() == UserRole.WALKER && walk.walker().id().equals(actorId);
        if (offered || own) return walk;
        if (actor.getRole() == UserRole.CLIENT && walkSearchRepository.hasActivePet(walkId, actorId)) return walk;
        throw new ResourceNotFoundException("Paseo no encontrado.");
    }

    private User activeUser(UUID id) {
        return users.findById(id).filter(User::isActive)
                .orElseThrow(() -> new ForbiddenActionException("Necesitás una cuenta activa para consultar los paseos."));
    }
}
