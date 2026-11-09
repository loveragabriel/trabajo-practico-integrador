package com.tpi.paseadores.pet.controller;

import com.tpi.paseadores.pet.domain.PetSize;
import com.tpi.paseadores.pet.dto.PetResponse;
import com.tpi.paseadores.pet.service.PetService;
import com.tpi.paseadores.shared.config.SecurityConfig;
import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.security.JwtAuthenticationFilter;
import com.tpi.paseadores.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PetController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = "app.jwt.secret=test-secret-not-for-production-use")
class PetControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtService jwt;
    @MockBean
    private PetService service;

    @Test
    void requiresAToken() throws Exception {
        mvc.perform(get("/api/pets")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void listsThePetsOfTheAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        when(service.getAll(userId)).thenReturn(List.of(new PetResponse(UUID.randomUUID(), "Firulais",
                "Mestizo", PetSize.MEDIUM, null, LocalDateTime.of(2026, 1, 1, 10, 0))));

        mvc.perform(get("/api/pets").header("Authorization", bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Firulais"))
                .andExpect(jsonPath("$[0].size").value("MEDIUM"))
                .andExpect(jsonPath("$[0].ownerId").doesNotExist());
    }

    @Test
    void createsAPetForTheAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        when(service.create(eq(userId), any())).thenReturn(new PetResponse(UUID.randomUUID(), "Firulais",
                null, PetSize.SMALL, null, LocalDateTime.now()));

        mvc.perform(post("/api/pets").header("Authorization", bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Firulais","size":"SMALL"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Firulais"));
        verify(service).create(eq(userId), any());
    }

    @Test
    void rejectsAPetWithoutName() throws Exception {
        mvc.perform(post("/api/pets").header("Authorization", bearer(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","size":"MEDIUM"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsAnUnknownSize() throws Exception {
        mvc.perform(post("/api/pets").header("Authorization", bearer(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Firulais","size":"HUGE"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void translatesSomeoneElsesPetToForbidden() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        when(service.update(eq(userId), eq(petId), any()))
                .thenThrow(new ForbiddenActionException("This pet belongs to another user"));

        mvc.perform(put("/api/pets/{id}", petId).header("Authorization", bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Robado"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This pet belongs to another user"));
    }

    private String bearer(UUID userId) {
        return "Bearer " + jwt.generateToken(userId.toString(), "OWNER");
    }
}
