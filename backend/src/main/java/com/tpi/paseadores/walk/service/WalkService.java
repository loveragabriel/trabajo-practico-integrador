package com.tpi.paseadores.walk.service;

import com.tpi.paseadores.pet.service.PetService;
import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.InvalidFieldException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.user.domain.User;
import com.tpi.paseadores.user.domain.UserRole;
import com.tpi.paseadores.user.service.UserService;
import com.tpi.paseadores.user.service.WalkerCoverageService;
import com.tpi.paseadores.walk.domain.BookingStatus;
import com.tpi.paseadores.walk.domain.Walk;
import com.tpi.paseadores.walk.domain.WalkBooking;
import com.tpi.paseadores.walk.dto.BookingResponse;
import com.tpi.paseadores.walk.domain.WalkStatus;
import com.tpi.paseadores.walk.dto.WalkAgendaPeriod;
import com.tpi.paseadores.walk.dto.WalkAgendaResponse;
import com.tpi.paseadores.walk.dto.WalkCreateRequest;
import com.tpi.paseadores.walk.dto.WalkReadResponse;
import com.tpi.paseadores.walk.dto.WalkResponse;
import com.tpi.paseadores.walk.dto.WalkSearchFilters;
import com.tpi.paseadores.walk.dto.WalkSearchResponse;
import com.tpi.paseadores.walk.dto.WalkSearchPageResponse;
import com.tpi.paseadores.walk.repository.WalkBookingRepository;
import com.tpi.paseadores.walk.repository.WalkRepository;
import com.tpi.paseadores.walk.repository.WalkSearchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class WalkService {
    private final WalkRepository walkRepository;
    private final WalkSearchRepository walkSearchRepository;
    private final WalkerCoverageService coverage;
    private final UserService users;
    private final Clock clock;
    private final WalkBookingRepository bookings;
    private final PetService pets;

    public WalkService(WalkRepository walkRepository, WalkSearchRepository walkSearchRepository,
                        WalkerCoverageService coverage, UserService users, Clock clock,
                        WalkBookingRepository bookings, PetService pets) {
        this.walkRepository = walkRepository;
        this.walkSearchRepository = walkSearchRepository;
        this.coverage = coverage;
        this.users = users;
        this.clock = clock;
        this.bookings = bookings;
        this.pets = pets;
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

    @Transactional(readOnly = true)
    public List<WalkSearchResponse> byWalker(UUID walkerId) {
        coverage.getPublicProfile(walkerId);
        return walkSearchRepository.findAvailable(new WalkSearchFilters(null, null, null, 0, 6),
                LocalDateTime.now(clock), walkerId).stream().limit(6).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> bookings(UUID actorId, UUID walkId, int page) {
        requireRole(actorId, UserRole.CLIENT);
        if (page < 0) throw new InvalidFieldException("page", "La página no puede ser negativa.");
        return walkSearchRepository.findBookings(actorId, walkId, LocalDateTime.now(clock), page,
                walkId == null ? 25 : Integer.MAX_VALUE);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void book(UUID actorId, UUID walkId, List<UUID> petIds) {
        requireRole(actorId, UserRole.CLIENT);
        if (new HashSet<>(petIds).size() != petIds.size()) {
            throw new InvalidFieldException("petIds", "Seleccioná cada mascota una sola vez.");
        }
        Walk walk = lockedWalk(walkId);
        LocalDateTime now = LocalDateTime.now(clock);
        if (!open(walk) || !walk.getStartTime().isAfter(now)) {
            throw new BusinessRuleViolationException("Este paseo ya no admite reservas.");
        }
        if (users.findById(walk.getWalkerId()).filter(User::isActive).isEmpty()) {
            throw new ResourceNotFoundException("Paseo no encontrado.");
        }
        for (UUID petId : petIds) pets.getPetEntity(actorId, petId);
        var existing = new HashMap<UUID, WalkBooking>();
        for (WalkBooking booking : bookings.findAllByWalkIdAndPetIdIn(walkId, petIds)) {
            if (booking.getStatus() == BookingStatus.JOINED) {
                throw new BusinessRuleViolationException("Una de esas mascotas ya tiene reserva.");
            }
            existing.put(booking.getPetId(), booking);
        }
        long occupied = bookings.countByWalkIdAndStatus(walkId, BookingStatus.JOINED);
        if (occupied + petIds.size() > walk.getCapacity()) {
            throw new BusinessRuleViolationException("No quedan lugares suficientes para esas mascotas.");
        }
        for (UUID petId : petIds) {
            WalkBooking prior = existing.get(petId);
            if (prior == null) bookings.save(new WalkBooking(walkId, petId, now));
            else prior.rejoin(now);
        }
        walk.updateAvailability(occupied + petIds.size());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancelBooking(UUID actorId, UUID walkId, UUID bookingId) {
        requireRole(actorId, UserRole.CLIENT);
        Walk walk = lockedWalk(walkId);
        WalkBooking booking = bookings.findByIdAndWalkId(bookingId, walkId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada."));
        pets.getPetEntity(actorId, booking.getPetId());
        if (booking.getStatus() == BookingStatus.CANCELLED) return;
        if (!open(walk) || walk.getStartTime().isBefore(LocalDateTime.now(clock).plusHours(2))) {
            throw new BusinessRuleViolationException("Podés cancelar hasta dos horas antes del paseo.");
        }
        long occupied = bookings.countByWalkIdAndStatus(walkId, BookingStatus.JOINED);
        booking.cancel();
        walk.updateAvailability(occupied - 1);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancelWalk(UUID actorId, UUID id) {
        Walk walk = ownLockedWalk(actorId, id);
        if (walk.getStatus() == WalkStatus.CANCELLED) return;
        if (!open(walk) || !walk.getStartTime().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessRuleViolationException("Solo se puede cancelar un paseo que todavía no empezó.");
        }
        bookings.findAllByWalkIdAndStatus(id, BookingStatus.JOINED).forEach(WalkBooking::cancel);
        walk.cancel();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void startWalk(UUID actorId, UUID id) {
        Walk walk = ownLockedWalk(actorId, id);
        if (walk.getStatus() == WalkStatus.IN_PROGRESS) return;
        LocalDateTime now = LocalDateTime.now(clock);
        if (!open(walk) || now.isBefore(walk.getStartTime()) || !now.isBefore(walk.getEndTime())) {
            throw new BusinessRuleViolationException("El paseo solo puede iniciarse dentro del horario publicado.");
        }
        walk.start();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void completeWalk(UUID actorId, UUID id) {
        Walk walk = ownLockedWalk(actorId, id);
        if (walk.getStatus() == WalkStatus.COMPLETED) return;
        if ((!open(walk) && walk.getStatus() != WalkStatus.IN_PROGRESS)
                || LocalDateTime.now(clock).isBefore(walk.getEndTime())) {
            throw new BusinessRuleViolationException("Solo podés completar un paseo una vez finalizado su horario.");
        }
        walk.complete();
    }

    @Transactional(readOnly = true)
    public UUID reviewableWalker(UUID actorId, UUID walkId) {
        requireRole(actorId, UserRole.CLIENT);
        Walk walk = walkRepository.findById(walkId)
                .orElseThrow(() -> new ResourceNotFoundException("Paseo no encontrado."));
        if (walk.getStatus() != WalkStatus.COMPLETED) {
            throw new BusinessRuleViolationException("Solo podés calificar paseos finalizados.");
        }
        if (!walkSearchRepository.hasActivePet(walkId, actorId)) {
            throw new ForbiddenActionException("Solo podés calificar un paseo en el que participaste.");
        }
        return walk.getWalkerId();
    }

    private Walk lockedWalk(UUID id) {
        return walkRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paseo no encontrado."));
    }

    private Walk ownLockedWalk(UUID actorId, UUID id) {
        requireRole(actorId, UserRole.WALKER);
        Walk walk = lockedWalk(id);
        if (!walk.isOwnedBy(actorId)) throw new ForbiddenActionException("Este paseo pertenece a otro paseador.");
        return walk;
    }

    private static boolean open(Walk walk) {
        return walk.getStatus() == WalkStatus.PUBLISHED || walk.getStatus() == WalkStatus.FULL;
    }

    private void requireRole(UUID actorId, UserRole role) {
        if (activeUser(actorId).getRole() != role) {
            throw new ForbiddenActionException("Tu tipo de cuenta no permite esta operación.");
        }
    }

    private User activeUser(UUID id) {
        return users.findById(id).filter(User::isActive)
                .orElseThrow(() -> new ForbiddenActionException("Necesitás una cuenta activa para consultar los paseos."));
    }
}
