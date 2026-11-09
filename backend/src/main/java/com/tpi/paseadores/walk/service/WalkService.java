package com.tpi.paseadores.walk.service;

import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.InvalidFieldException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.shared.dto.PageResponse;
import com.tpi.paseadores.shared.validation.Pagination;
import com.tpi.paseadores.user.domain.User;
import com.tpi.paseadores.user.domain.UserRole;
import com.tpi.paseadores.user.service.UserService;
import com.tpi.paseadores.user.service.WalkerCoverageService;
import com.tpi.paseadores.walk.domain.Walk;
import com.tpi.paseadores.walk.domain.WalkStatus;
import com.tpi.paseadores.walk.dto.WalkAgendaPeriod;
import com.tpi.paseadores.walk.dto.WalkCreateRequest;
import com.tpi.paseadores.walk.dto.WalkReadResponse;
import com.tpi.paseadores.walk.dto.WalkSearchFilters;
import com.tpi.paseadores.walk.repository.WalkRepository;
import com.tpi.paseadores.walk.repository.WalkSearchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// el Walk en sí: alta, búsqueda, agenda, detalle y su propio ciclo de estados (RN-29/31).
// Reservar/cancelar/consultar reservas es un caso de uso aparte: ver BookingService.
@Service
public class WalkService {
    private final WalkRepository walkRepository;
    private final WalkPetService walkPets;
    private final WalkSearchRepository walkSearchRepository;
    private final WalkerCoverageService coverage;
    private final UserService users;
    private final Clock clock;

    public WalkService(WalkRepository walkRepository, WalkPetService walkPets,
                        WalkSearchRepository walkSearchRepository, WalkerCoverageService coverage,
                        UserService users, Clock clock) {
        this.walkRepository = walkRepository;
        this.walkPets = walkPets;
        this.walkSearchRepository = walkSearchRepository;
        this.coverage = coverage;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalkReadResponse create(UUID walkerId, WalkCreateRequest request) {
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
        return walkSearchRepository.findVisibleDetail(walk.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Paseo no encontrado."));
    }

    @Transactional(readOnly = true)
    public PageResponse<WalkReadResponse> search(UUID actorId, WalkSearchFilters filters) {
        Pagination.validate(filters.page(), filters.size());
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
        return new PageResponse<>(items, filters.page(), filters.size(), hasNext);
    }

    @Transactional(readOnly = true)
    public PageResponse<WalkReadResponse> mine(UUID actorId, WalkAgendaPeriod period, int page, int size) {
        User actor = activeUser(actorId);
        if (actor.getRole() != UserRole.WALKER) {
            throw new ForbiddenActionException("La agenda está disponible para paseadores activos.");
        }
        Pagination.validate(page, size);
        var rows = walkSearchRepository.findAgenda(actorId, period, LocalDateTime.now(clock), page, size);
        return new PageResponse<>(List.copyOf(rows.subList(0, Math.min(rows.size(), size))),
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

    // RN-29/31: cambia el estado del paseo. Las transiciones automáticas PUBLISHED<->FULL
    // (RN-30) no se pueden forzar acá. RN-32/33 las resuelve WalkPetService.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void updateStatus(UUID actorId, UUID walkId, WalkStatus target) {
        if (target != WalkStatus.CANCELLED && target != WalkStatus.IN_PROGRESS && target != WalkStatus.COMPLETED) {
            throw new InvalidFieldException("status", "Ese estado no se puede forzar manualmente.");
        }
        Walk walk = ownLockedWalk(actorId, walkId);
        if (!walk.getStatus().canTransitionTo(target)) {
            throw new BusinessRuleViolationException("No se puede pasar de " + walk.getStatus() + " a " + target + ".");
        }
        if (target == WalkStatus.IN_PROGRESS && !walkPets.hasAnyJoined(walkId)) {
            throw new BusinessRuleViolationException("No se puede iniciar un paseo sin mascotas inscriptas.");
        }
        if (target == WalkStatus.CANCELLED) {
            walkPets.cancelAllJoined(walkId);
        }
        walk.transitionTo(target);
    }

    private User activeUser(UUID id) {
        return users.findById(id).filter(User::isActive)
                .orElseThrow(() -> new ForbiddenActionException("Necesitás una cuenta activa para consultar los paseos."));
    }

    private void requireRole(UUID actorId, UserRole role) {
        User actor = activeUser(actorId);
        if (actor.getRole() != role) {
            throw new ForbiddenActionException("No tenés permiso para realizar esta acción.");
        }
    }

    // RN-31: solo el paseador dueño puede cambiar el estado de su paseo
    private Walk ownLockedWalk(UUID actorId, UUID walkId) {
        requireRole(actorId, UserRole.WALKER);
        Walk walk = walkRepository.findByIdForUpdate(walkId)
                .orElseThrow(() -> new ResourceNotFoundException("Paseo no encontrado."));
        if (!walk.isOwnedBy(actorId)) {
            throw new ForbiddenActionException("No tenés permiso para modificar este paseo.");
        }
        return walk;
    }
}
