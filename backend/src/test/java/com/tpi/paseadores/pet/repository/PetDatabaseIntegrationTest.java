package com.tpi.paseadores.pet.repository;

import com.tpi.paseadores.auth.service.AuthService;
import com.tpi.paseadores.auth.dto.RegisterRequest;
import com.tpi.paseadores.pet.domain.PetSize;
import com.tpi.paseadores.pet.dto.PetRequest;
import com.tpi.paseadores.pet.dto.PetResponse;
import com.tpi.paseadores.pet.service.PetService;
import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.zone.repository.ZoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Opt in with a dedicated PostgreSQL database and the application's DB/JWT variables.
 * Confirms the `pet_size` native enum round-trips and that each owner only sees their own pets.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "TPI_DB_TESTS", matches = "true")
class PetDatabaseIntegrationTest {
    @Autowired
    private PetService service;
    @Autowired
    private AuthService authService;
    @Autowired
    private ZoneRepository zoneRepository;

    @Test
    void ownerSeesOnlyTheirPetsAgainstTheRealSchema() {
        UUID ana = registerClient();
        UUID beto = registerClient();

        PetResponse created = service.create(ana, new PetRequest("Firulais", "Mestizo", PetSize.LARGE, null));
        service.create(beto, new PetRequest("Luna", null, null, null));

        assertThat(service.getAll(ana)).extracting(PetResponse::id).containsExactly(created.id());
        assertThat(service.getById(ana, created.id()).size()).isEqualTo(PetSize.LARGE);
        assertThatThrownBy(() -> service.getPetEntity(beto, created.id()))
                .isInstanceOf(ForbiddenActionException.class);
    }

    private UUID registerClient() {
        UUID zoneId = zoneRepository.findAllByOrderByNameAsc().getFirst().getId();
        return authService.register(new RegisterRequest("Integration Client",
                "client-" + UUID.randomUUID() + "@example.com", "password123", null, "CLIENT", zoneId))
                .user().id();
    }
}
