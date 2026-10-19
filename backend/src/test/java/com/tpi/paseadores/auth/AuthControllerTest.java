package com.tpi.paseadores.auth;

import com.tpi.paseadores.auth.dto.AuthResponse;
import com.tpi.paseadores.auth.dto.UserSummary;
import com.tpi.paseadores.shared.config.SecurityConfig;
import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.shared.exception.InvalidCredentialsException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = "app.jwt.secret=test-secret-not-for-production-use")
class AuthControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockBean
    private AuthService service;

    @Test
    void registerDoesNotRequireAToken() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.register(any())).thenReturn(new AuthResponse("jwt-token", new UserSummary(id, "Ana", "CLIENT")));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"Ana Dueña","email":"ana@example.com","password":"password123",
                 "role":"CLIENT","zoneId":"%s"}
                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.role").value("CLIENT"));
    }

    @Test
    void loginDoesNotRequireAToken() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.login(any())).thenReturn(new AuthResponse("jwt-token", new UserSummary(id, "Ana", "CLIENT")));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"ana@example.com","password":"password123"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void rejectsRegistrationWithInvalidFields() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"","email":"not-an-email","password":"short","role":"ADMIN"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void translatesDuplicateEmailToConflict() throws Exception {
        when(service.register(any())).thenThrow(new BusinessRuleViolationException("Email already registered"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"Ana Dueña","email":"ana@example.com","password":"password123",
                 "role":"CLIENT","zoneId":"%s"}
                """.formatted(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void translatesUnknownZoneToNotFound() throws Exception {
        when(service.register(any())).thenThrow(new ResourceNotFoundException("Zone not found"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"Ana Dueña","email":"ana@example.com","password":"password123",
                 "role":"CLIENT","zoneId":"%s"}
                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void translatesBadCredentialsToUnauthorized() throws Exception {
        when(service.login(any())).thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"ana@example.com","password":"wrong-pass"}
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
