package com.tpi.paseadores.walk.service;

import com.tpi.paseadores.pet.domain.Pet;
import com.tpi.paseadores.pet.service.PetService;
import com.tpi.paseadores.shared.dto.PageResponse;
import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.shared.validation.Pagination;
import com.tpi.paseadores.user.domain.User;
import com.tpi.paseadores.user.domain.UserRole;
import com.tpi.paseadores.user.service.UserService;
import com.tpi.paseadores.walk.domain.Walk;
import com.tpi.paseadores.walk.dto.BookingResponse;
import com.tpi.paseadores.walk.dto.ClientBookingResponse;
import com.tpi.paseadores.walk.dto.WalkAgendaPeriod;
import com.tpi.paseadores.walk.repository.WalkRepository;
import com.tpi.paseadores.walk.repository.WalkSearchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// el caso de uso completo de "un cliente reserva/cancela/consulta su reserva" (RN-21/22/40 +
// lo que resuelve WalkPetService). No es el service de ninguna entidad propia (como
// WalkerCoverageService tampoco lo es): orquesta Walk + Pet + WalkPet para esta acción puntual.
@Service
public class BookingService {
    private final WalkRepository walkRepository;
    private final WalkPetService walkPets;
    private final WalkSearchRepository walkSearchRepository;
    private final UserService users;
    private final PetService pets;
    private final Clock clock;

    public BookingService(WalkRepository walkRepository, WalkPetService walkPets,
                           WalkSearchRepository walkSearchRepository, UserService users,
                           PetService pets, Clock clock) {
        this.walkRepository = walkRepository;
        this.walkPets = walkPets;
        this.walkSearchRepository = walkSearchRepository;
        this.users = users;
        this.pets = pets;
        this.clock = clock;
    }

    // RN-21/22/40: resuelve rol, dueño de la mascota y lock del paseo; RN-23/24/25/26 las
    // valida WalkPetService
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse join(UUID actorId, UUID walkId, UUID petId) {
        requireRole(actorId, UserRole.CLIENT);
        Pet pet = pets.getPetEntity(actorId, petId);
        Walk walk = walkRepository.findByIdForUpdate(walkId)
                .orElseThrow(() -> new ResourceNotFoundException("Paseo no encontrado."));
        return walkPets.join(walk, pet);
    }

    // RN-21/22/40: idem join(); RN-27/28 las valida WalkPetService
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancelBooking(UUID actorId, UUID walkId, UUID petId) {
        requireRole(actorId, UserRole.CLIENT);
        pets.getPetEntity(actorId, petId);
        Walk walk = walkRepository.findByIdForUpdate(walkId)
                .orElseThrow(() -> new ResourceNotFoundException("Paseo no encontrado."));
        walkPets.cancel(walk, petId);
    }

    // "mis reservas" del cliente: simétrico a WalkService.mine() del paseador
    @Transactional(readOnly = true)
    public PageResponse<ClientBookingResponse> myBookings(UUID actorId, WalkAgendaPeriod period, int page, int size) {
        requireRole(actorId, UserRole.CLIENT);
        Pagination.validate(page, size);
        var rows = walkSearchRepository.findBookingsForClient(actorId, period, LocalDateTime.now(clock), page, size);
        return new PageResponse<>(List.copyOf(rows.subList(0, Math.min(rows.size(), size))),
                page, size, rows.size() > size);
    }

    private void requireRole(UUID actorId, UserRole role) {
        User actor = users.findById(actorId).filter(User::isActive)
                .orElseThrow(() -> new ForbiddenActionException("Necesitás una cuenta activa para realizar esta acción."));
        if (actor.getRole() != role) {
            throw new ForbiddenActionException("No tenés permiso para realizar esta acción.");
        }
    }
}
