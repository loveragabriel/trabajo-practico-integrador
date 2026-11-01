package com.tpi.paseadores.pet.dto;

import com.tpi.paseadores.pet.PetSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// same body for create and update. There is no ownerId: the owner is always the
// authenticated user, so a pet can't be created for, or moved to, someone else.
public record PetRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String breed,
        PetSize size,
        @Size(max = 1000) String notes
) {
}
