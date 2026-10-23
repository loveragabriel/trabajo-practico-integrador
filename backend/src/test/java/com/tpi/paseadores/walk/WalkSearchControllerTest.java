package com.tpi.paseadores.walk;

import com.tpi.paseadores.shared.config.SecurityConfig;
import com.tpi.paseadores.shared.security.JwtAuthenticationFilter;
import com.tpi.paseadores.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalkSearchController.class)
@Import({WalkSearchSecurityConfig.class, SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = "app.jwt.secret=test-secret-not-for-production-use")
class WalkSearchControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean WalkSearchService service;

    @Test
    void guestCanReadOnlyPublicWalkData() throws Exception {
        var row = new WalkSearchResponse(UUID.randomUUID(), UUID.randomUUID(), "Palermo",
                LocalDateTime.of(2030, 1, 1, 10, 0), 60, "Plaza", new BigDecimal("2500"), 3, 2,
                new WalkSearchResponse.WalkerSummary(UUID.randomUUID(), "Paseador", new BigDecimal("4.5"), 2));
        when(service.search(any())).thenReturn(new WalkSearchPageResponse(List.of(row), 0, 12, false));
        mvc.perform(get("/api/walks")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].availableSpots").value(2))
                .andExpect(jsonPath("$.items[0].walker.name").value("Paseador"))
                .andExpect(jsonPath("$.items[0].walker.email").doesNotExist())
                .andExpect(jsonPath("$.items[0].walker.phone").doesNotExist())
                .andExpect(jsonPath("$.items[0].walker.passwordHash").doesNotExist());
    }

    @Test
    void passesTypedFiltersToTheService() throws Exception {
        var zone = UUID.randomUUID();
        var from = LocalDateTime.of(2030, 1, 1, 10, 0);
        var filters = new WalkSearchFilters(zone, from, from.plusHours(2), 1, 5);
        when(service.search(filters)).thenReturn(new WalkSearchPageResponse(List.of(), 1, 5, false));
        mvc.perform(get("/api/walks").param("zoneId", zone.toString()).param("startsFrom", from.toString())
                .param("startsTo", from.plusHours(2).toString()).param("page", "1").param("size", "5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1));
        verify(service).search(filters);
    }

    @Test
    void rejectsMalformedFiltersWithTheExistingErrorShape() throws Exception {
        for (String[] invalid : List.of(new String[]{"zoneId", "no-es-uuid"},
                new String[]{"startsFrom", "2026-02-31T10:00"}, new String[]{"page", "abc"})) {
            mvc.perform(get("/api/walks").param(invalid[0], invalid[1])).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value(invalid[0]));
        }
        verifyNoInteractions(service);
    }

    @Test
    void invalidRangeIsBadRequest() throws Exception {
        when(service.search(any())).thenThrow(new InvalidWalkSearchException("startsTo", "Hasta debe ser posterior a Desde."));
        mvc.perform(get("/api/walks")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("startsTo"));
    }

    @Test
    void publicAccessDoesNotOpenWritesOrOtherWalkRoutes() throws Exception {
        mvc.perform(post("/api/walks")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/walks/mine")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/walks/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void authenticatedUnsupportedWriteIsStillNotAnEndpoint() throws Exception {
        String token = jwt.generateToken(UUID.randomUUID().toString(), "WALKER");
        mvc.perform(post("/api/walks").header("Authorization", "Bearer " + token))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(service);
    }
}
