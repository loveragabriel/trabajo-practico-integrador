package com.tpi.paseadores.walk.dto;

import com.tpi.paseadores.pet.domain.PetSize;

import java.util.UUID;

// una fila de "perros de esta salida": solo la ve el paseador dueño del walk (WalkService),
// para coordinar antes de salir
public record JoinedPetResponse(UUID petId, String petName, String breed, PetSize size, String notes,
                                 OwnerSummary owner) {
    public record OwnerSummary(String name, String phone) {}
}
