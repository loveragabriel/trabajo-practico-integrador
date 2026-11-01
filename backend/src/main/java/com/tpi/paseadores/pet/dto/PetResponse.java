package com.tpi.paseadores.pet.dto;

import com.tpi.paseadores.pet.Pet;
import com.tpi.paseadores.pet.PetSize;

import java.time.LocalDateTime;
import java.util.UUID;

public record PetResponse(UUID id, String name, String breed, PetSize size, String notes, LocalDateTime createdAt) {
    public static PetResponse from(Pet pet) {
        return new PetResponse(pet.getId(), pet.getName(), pet.getBreed(), pet.getSize(), pet.getNotes(),
                pet.getCreatedAt());
    }
}
