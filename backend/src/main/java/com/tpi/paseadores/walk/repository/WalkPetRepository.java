package com.tpi.paseadores.walk.repository;

import com.tpi.paseadores.walk.domain.BookingStatus;
import com.tpi.paseadores.walk.domain.WalkPet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalkPetRepository extends JpaRepository<WalkPet, UUID> {

    // RN-25/RN-26: busca la fila existente para revivirla en vez de insertar una nueva
    Optional<WalkPet> findByWalkIdAndPetId(UUID walkId, UUID petId);

    // RN-24: cupos libres = capacity - JOINED. Se llama bajo el lock de Walk (findByIdForUpdate).
    long countByWalkIdAndStatus(UUID walkId, BookingStatus status);

    // RN-32: todas las reservas activas a cancelar cuando el paseador cancela el paseo
    List<WalkPet> findAllByWalkIdAndStatus(UUID walkId, BookingStatus status);

    // usado por review: solo quien participó (una mascota propia JOINED en el paseo) puede calificar
    boolean existsByWalkIdAndPetIdInAndStatus(UUID walkId, List<UUID> petIds, BookingStatus status);
}
