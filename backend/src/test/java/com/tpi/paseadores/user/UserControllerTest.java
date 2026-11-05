package com.tpi.paseadores.user;

import com.tpi.paseadores.shared.config.SecurityConfig;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.shared.security.JwtAuthenticationFilter;
import com.tpi.paseadores.shared.security.JwtService;
import com.tpi.paseadores.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = "app.jwt.secret=test-secret-not-for-production-use")
class UserControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtService jwt;
    @MockBean
    private UserService service;

    @Test
    void requiresAToken() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void getsTheProfileOfTheAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        when(service.getProfile(userId)).thenReturn(new UserResponse(userId, "Firulais",
                "firulais@example.com", null, UserRole.CLIENT, UUID.randomUUID(), true,
                null, null, BigDecimal.ZERO, 0, LocalDateTime.of(2026, 1, 1, 10, 0)));

        mvc.perform(get("/api/users/me").header("Authorization", bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Firulais"))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    void updatesTheProfileOfTheAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        when(service.updateProfile(eq(userId), any())).thenReturn(new UserResponse(userId, "Gabriel Lovera",
                "firulais@example.com", "1122334455", UserRole.CLIENT, zoneId, true,
                null, null, BigDecimal.ZERO, 0, LocalDateTime.now()));

        mvc.perform(put("/api/users/me").header("Authorization", bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Gabriel Lovera","phone":"1122334455","zoneId":"%s"}
                                """.formatted(zoneId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Gabriel Lovera"))
                .andExpect(jsonPath("$.zoneId").value(zoneId.toString()));
        verify(service).updateProfile(eq(userId), any());
    }

    @Test
    void rejectsAProfileWithoutFullName() throws Exception {
        mvc.perform(put("/api/users/me").header("Authorization", bearer(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":" ","zoneId":"%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("fullName"));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsAProfileWithoutZone() throws Exception {
        mvc.perform(put("/api/users/me").header("Authorization", bearer(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Gabriel Lovera"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("zoneId"));
        verifyNoInteractions(service);
    }

    @Test
    void translatesAnUnknownZoneToNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        when(service.updateProfile(eq(userId), any()))
                .thenThrow(new ResourceNotFoundException("Zone not found"));

        mvc.perform(put("/api/users/me").header("Authorization", bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Gabriel Lovera","zoneId":"%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Zone not found"));
    }

    private String bearer(UUID userId) {
        return "Bearer " + jwt.generateToken(userId.toString(), "CLIENT");
    }
}
