package com.tpi.paseadores.pet.service;

import com.tpi.paseadores.pet.domain.Pet;
import com.tpi.paseadores.pet.domain.PetSize;
import com.tpi.paseadores.pet.dto.PetRequest;
import com.tpi.paseadores.pet.dto.PetResponse;
import com.tpi.paseadores.pet.repository.PetRepository;
import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.user.domain.User;
import com.tpi.paseadores.user.domain.UserRole;
import com.tpi.paseadores.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {
    @Mock
    private PetRepository petRepository;
    @Mock
    private UserService userService;

    private PetService service;

    @BeforeEach
    void setUp() {
        service = new PetService(petRepository, userService);
    }

    @Test
    void listsOnlyThePetsOfTheGivenOwner() {
        UUID userId = UUID.randomUUID();
        when(petRepository.findAllByOwnerIdOrderByCreatedAtAsc(userId))
                .thenReturn(List.of(pet(userId, "Firulais"), pet(userId, "Luna")));

        List<PetResponse> pets = service.getAll(userId);

        assertThat(pets).extracting(PetResponse::name).containsExactly("Firulais", "Luna");
    }

    @Test
    void createsAPetForAnActiveOwner() {
        UUID userId = UUID.randomUUID();
        when(userService.findById(userId)).thenReturn(Optional.of(user(userId, UserRole.CLIENT, true)));
        when(petRepository.save(any(Pet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PetResponse response = service.create(userId,
                new PetRequest("  Firulais ", " ", PetSize.MEDIUM, "Tira de la correa"));

        ArgumentCaptor<Pet> captor = ArgumentCaptor.forClass(Pet.class);
        verify(petRepository).save(captor.capture());
        Pet saved = captor.getValue();
        assertThat(saved.getOwnerId()).isEqualTo(userId);
        assertThat(saved.getName()).isEqualTo("Firulais");
        assertThat(saved.getBreed()).isNull();
        assertThat(saved.getSize()).isEqualTo(PetSize.MEDIUM);
        assertThat(response.notes()).isEqualTo("Tira de la correa");
    }

    @Test
    void rejectsPetCreationByAWalker() {
        UUID userId = UUID.randomUUID();
        when(userService.findById(userId)).thenReturn(Optional.of(user(userId, UserRole.WALKER, true)));

        assertThatThrownBy(() -> service.create(userId, new PetRequest("Firulais", null, null, null)))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("Only clients can register pets");
        verify(petRepository, never()).save(any());
    }

    @Test
    void rejectsPetCreationByADeactivatedOwner() {
        UUID userId = UUID.randomUUID();
        when(userService.findById(userId)).thenReturn(Optional.of(user(userId, UserRole.CLIENT, false)));

        assertThatThrownBy(() -> service.create(userId, new PetRequest("Firulais", null, null, null)))
                .isInstanceOf(ForbiddenActionException.class);
        verify(petRepository, never()).save(any());
    }

    @Test
    void ownerUpdatesTheirPetButNotItsOwner() {
        UUID userId = UUID.randomUUID();
        Pet pet = pet(userId, "Firulais");
        when(petRepository.findById(pet.getId())).thenReturn(Optional.of(pet));

        PetResponse response = service.update(userId, pet.getId(),
                new PetRequest("Firu", "Mestizo", PetSize.SMALL, null));

        assertThat(response.name()).isEqualTo("Firu");
        assertThat(response.breed()).isEqualTo("Mestizo");
        assertThat(pet.getOwnerId()).isEqualTo(userId);
    }

    @Test
    void rejectsEditingSomeoneElsesPet() {
        Pet pet = pet(UUID.randomUUID(), "Firulais");
        when(petRepository.findById(pet.getId())).thenReturn(Optional.of(pet));

        assertThatThrownBy(() -> service.update(UUID.randomUUID(), pet.getId(),
                new PetRequest("Robado", null, null, null)))
                .isInstanceOf(ForbiddenActionException.class);
        assertThat(pet.getName()).isEqualTo("Firulais");
    }

    // acceptance criterion: the booking flow goes through getPetEntity, so a foreign pet can't be booked
    @Test
    void rejectsUsingSomeoneElsesPetForABooking() {
        Pet pet = pet(UUID.randomUUID(), "Firulais");
        when(petRepository.findById(pet.getId())).thenReturn(Optional.of(pet));

        assertThatThrownBy(() -> service.getPetEntity(UUID.randomUUID(), pet.getId()))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("This pet belongs to another user");
    }

    @Test
    void returnsTheOwnedPetForABooking() {
        UUID userId = UUID.randomUUID();
        Pet pet = pet(userId, "Firulais");
        when(petRepository.findById(pet.getId())).thenReturn(Optional.of(pet));

        assertThat(service.getPetEntity(userId, pet.getId())).isSameAs(pet);
    }

    @Test
    void reportsAnUnknownPetAsNotFound() {
        UUID petId = UUID.randomUUID();
        when(petRepository.findById(petId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(UUID.randomUUID(), petId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Pet not found");
    }

    private static Pet pet(UUID ownerId, String name) {
        Pet pet = new Pet(ownerId);
        pet.setId(UUID.randomUUID());
        pet.setName(name);
        return pet;
    }

    private static User user(UUID id, UserRole role, boolean active) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setActive(active);
        return user;
    }
}
