package com.tpi.paseadores.user;

import com.tpi.paseadores.shared.config.SecurityConfig;
import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.shared.security.JwtAuthenticationFilter;
import com.tpi.paseadores.shared.security.JwtService;
import com.tpi.paseadores.user.dto.WalkerCoverageResponse;
import com.tpi.paseadores.user.dto.WalkerPublicProfileResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
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

@WebMvcTest(controllers = WalkerController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = "app.jwt.secret=test-secret-not-for-production-use")
class WalkerControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtService jwt;
    @MockBean
    private WalkerCoverageService service;

    @Test
    void getsThePublicProfileWithoutAToken() throws Exception {
        UUID walkerId = UUID.randomUUID();
        when(service.getPublicProfile(walkerId)).thenReturn(new WalkerPublicProfileResponse(walkerId,
                "Paseador de prueba", new BigDecimal("2500"), 4, new BigDecimal("4.5"), 10, List.of()));

        mvc.perform(get("/api/walkers/{id}", walkerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Paseador de prueba"));
    }

    @Test
    void requiresATokenForOwnCoverage() throws Exception {
        mvc.perform(get("/api/walkers/me")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void getsOwnCoverageOfTheAuthenticatedWalker() throws Exception {
        UUID walkerId = UUID.randomUUID();
        when(service.getOwnCoverage(walkerId)).thenReturn(
                new WalkerCoverageResponse(new BigDecimal("2500"), 4, List.of()));

        mvc.perform(get("/api/walkers/me").header("Authorization", bearer(walkerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultCapacity").value(4));
    }

    @Test
    void updatesOwnCoverageOfTheAuthenticatedWalker() throws Exception {
        UUID walkerId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        when(service.updateOwnCoverage(eq(walkerId), any())).thenReturn(
                new WalkerCoverageResponse(new BigDecimal("3000"), 5, List.of()));

        mvc.perform(put("/api/walkers/me").header("Authorization", bearer(walkerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"defaultPrice":3000,"defaultCapacity":5,"zoneIds":["%s"]}
                                """.formatted(zoneId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultPrice").value(3000));
        verify(service).updateOwnCoverage(eq(walkerId), any());
    }

    @Test
    void rejectsAnUpdateWithoutZoneIds() throws Exception {
        mvc.perform(put("/api/walkers/me").header("Authorization", bearer(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"defaultPrice":3000,"defaultCapacity":5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("zoneIds"));
        verifyNoInteractions(service);
    }

    @Test
    void translatesAClientTryingToPublishCoverageToForbidden() throws Exception {
        UUID clientId = UUID.randomUUID();
        when(service.updateOwnCoverage(eq(clientId), any()))
                .thenThrow(new ForbiddenActionException("Only walkers can manage service coverage"));

        mvc.perform(put("/api/walkers/me").header("Authorization", bearer(clientId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"zoneIds":[]}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only walkers can manage service coverage"));
    }

    @Test
    void translatesAnUnknownZoneToNotFound() throws Exception {
        UUID walkerId = UUID.randomUUID();
        when(service.updateOwnCoverage(eq(walkerId), any()))
                .thenThrow(new ResourceNotFoundException("Zone not found"));

        mvc.perform(put("/api/walkers/me").header("Authorization", bearer(walkerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"zoneIds":["%s"]}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    private String bearer(UUID userId) {
        return "Bearer " + jwt.generateToken(userId.toString(), "WALKER");
    }
}
