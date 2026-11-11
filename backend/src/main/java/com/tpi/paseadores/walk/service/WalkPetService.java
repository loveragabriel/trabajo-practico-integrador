package com.tpi.paseadores.walk.service;

import com.tpi.paseadores.pet.domain.Pet;
import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.walk.domain.BookingStatus;
import com.tpi.paseadores.walk.domain.Walk;
import com.tpi.paseadores.walk.domain.WalkPet;
import com.tpi.paseadores.walk.domain.WalkStatus;
import com.tpi.paseadores.walk.dto.BookingResponse;
import com.tpi.paseadores.walk.repository.WalkPetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// reglas propias de la entidad walk_pets: RN-23 a RN-28, RN-32, RN-33. No conoce rol, lock ni
// paginación (eso lo resuelve BookingService); join() y cancel() reciben el Walk ya lockeado en
// vez de buscarlo. Sin dependencia hacia WalkService/BookingService: evita un ciclo con ellos.
@Service
public class WalkPetService {
    private final WalkPetRepository walkPetRepository;
    private final Clock clock;

    public WalkPetService(WalkPetRepository walkPetRepository, Clock clock) {
        this.walkPetRepository = walkPetRepository;
        this.clock = clock;
    }

    // RN-23/24/25/26: sumar una mascota a un paseo publicado
    @Transactional
    public BookingResponse join(Walk walk, Pet pet) {
        if (walk.getStatus() != WalkStatus.PUBLISHED) {
            throw new BusinessRuleViolationException("Este paseo ya no admite inscripciones.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (!walk.getStartTime().isAfter(now)) {
            throw new BusinessRuleViolationException("Ya pasó la hora de inicio de este paseo.");
        }

        long occupied = walkPetRepository.countByWalkIdAndStatus(walk.getId(), BookingStatus.JOINED);
        var existing = walkPetRepository.findByWalkIdAndPetId(walk.getId(), pet.getId());
        if (existing.isPresent() && existing.get().getStatus() == BookingStatus.JOINED) {
            throw new BusinessRuleViolationException("Esta mascota ya está inscripta en este paseo.");
        }
        if (occupied >= walk.getCapacity()) {
            throw new BusinessRuleViolationException("Ya no quedan lugares disponibles en este paseo.");
        }

        WalkPet booking;
        if (existing.isPresent()) {
            booking = existing.get();
            booking.rejoin(now);
        } else {
            booking = walkPetRepository.save(new WalkPet(walk.getId(), pet.getId(), now));
        }
        walk.updateAvailability(occupied + 1);
        return BookingResponse.from(booking, pet.getName());
    }

    // RN-27/28: cancela la inscripción hasta 2hs antes del horario del paseo
    @Transactional
    public void cancel(Walk walk, UUID petId) {
        WalkPet booking = walkPetRepository.findByWalkIdAndPetId(walk.getId(), petId)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos una reserva de esa mascota en este paseo."));
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return;
        }
        if (walk.getStatus() != WalkStatus.PUBLISHED && walk.getStatus() != WalkStatus.FULL) {
            throw new BusinessRuleViolationException("Ya no se puede cancelar la reserva de este paseo.");
        }
        if (!walk.getStartTime().isAfter(LocalDateTime.now(clock).plusHours(2))) {
            throw new BusinessRuleViolationException("Para cancelar, tienen que faltar más de dos horas para el paseo.");
        }

        long occupied = walkPetRepository.countByWalkIdAndStatus(walk.getId(), BookingStatus.JOINED);
        booking.cancel();
        walk.updateAvailability(occupied - 1);
    }

    // RN-32: el paseador cancela el paseo; se llama en la misma transacción que esa cancelación
    @Transactional
    public void cancelAllJoined(UUID walkId) {
        walkPetRepository.findAllByWalkIdAndStatus(walkId, BookingStatus.JOINED).forEach(WalkPet::cancel);
    }

    // RN-33: no se puede iniciar un paseo sin mascotas inscriptas
    @Transactional(readOnly = true)
    public boolean hasAnyJoined(UUID walkId) {
        return walkPetRepository.countByWalkIdAndStatus(walkId, BookingStatus.JOINED) > 0;
    }

    // usado por review: el autor participó si alguna de sus mascotas tiene una reserva JOINED en el paseo
    @Transactional(readOnly = true)
    public boolean hasJoinedBooking(UUID walkId, List<UUID> petIds) {
        return !petIds.isEmpty()
                && walkPetRepository.existsByWalkIdAndPetIdInAndStatus(walkId, petIds, BookingStatus.JOINED);
    }
}
