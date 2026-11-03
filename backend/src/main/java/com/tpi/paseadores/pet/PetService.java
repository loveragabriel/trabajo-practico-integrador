package com.tpi.paseadores.pet;

import com.tpi.paseadores.pet.dto.PetRequest;
import com.tpi.paseadores.pet.dto.PetResponse;
import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.shared.validation.StringUtils;
import com.tpi.paseadores.user.User;
import com.tpi.paseadores.user.UserRole;
import com.tpi.paseadores.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// public API of the pet feature. No delete in the MVP: a pet with booking history stays (see docs/modulos.md)
@Service
@RequiredArgsConstructor
public class PetService {
    private final PetRepository petRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<PetResponse> getAll(UUID userId) {
        return petRepository.findAllByOwnerIdOrderByCreatedAtAsc(userId).stream().map(PetResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PetResponse getById(UUID userId, UUID petId) {
        return PetResponse.from(getPetEntity(userId, petId));
    }

    @Transactional
    public PetResponse create(UUID userId, PetRequest request) {
        boolean isActiveClient = userService.findById(userId)
                .filter(u -> u.isActive() && u.getRole() == UserRole.CLIENT)
                .isPresent();
        if (!isActiveClient) {
            throw new ForbiddenActionException("Only clients can register pets");
        }

        Pet pet = new Pet(userId);
        apply(pet, request);
        return PetResponse.from(petRepository.save(pet));
    }

    // RN-10: only the owner edits; the owner itself never changes
    @Transactional
    public PetResponse update(UUID userId, UUID petId, PetRequest request) {
        Pet pet = getPetEntity(userId, petId);
        apply(pet, request);
        return PetResponse.from(pet);
    }

    // usado internamente y por booking: verifica que la mascota es del user (RN-22)
    public Pet getPetEntity(UUID userId, UUID petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));
        if (!pet.isOwnedBy(userId)) {
            throw new ForbiddenActionException("This pet belongs to another user");
        }
        return pet;
    }

    private static void apply(Pet pet, PetRequest request) {
        pet.setName(request.name().trim());
        pet.setBreed(StringUtils.blankToNull(request.breed()));
        pet.setSize(request.size());
        pet.setNotes(StringUtils.blankToNull(request.notes()));
    }
}
