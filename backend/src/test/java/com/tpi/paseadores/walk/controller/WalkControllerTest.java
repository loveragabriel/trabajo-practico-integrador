package com.tpi.paseadores.walk.controller;

import com.tpi.paseadores.shared.config.SecurityConfig;
import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.InvalidFieldException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.shared.security.JwtAuthenticationFilter;
import com.tpi.paseadores.shared.security.JwtService;
import com.tpi.paseadores.walk.domain.WalkStatus;
import com.tpi.paseadores.walk.dto.WalkAgendaPeriod;
import com.tpi.paseadores.walk.dto.WalkAgendaResponse;
import com.tpi.paseadores.walk.dto.WalkReadResponse;
import com.tpi.paseadores.walk.dto.WalkResponse;
import com.tpi.paseadores.walk.dto.WalkSearchFilters;
import com.tpi.paseadores.walk.dto.WalkSearchPageResponse;
import com.tpi.paseadores.walk.dto.WalkSearchResponse;
import com.tpi.paseadores.walk.service.WalkService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalkController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = "app.jwt.secret=clave-de-prueba-no-usar-en-produccion")
class WalkControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean WalkService service;

    @Test
    void everyRouteRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/walks").contentType("application/json").content(createBody(45, "2500.50")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/walks")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/walks/mine")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/walks/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    // --- crear ---------------------------------------------------------------------------------

    @Test
    void createsTheWalkForTheTokenActorAndReturnsItsLocation() throws Exception {
        UUID actor = UUID.randomUUID(), walk = UUID.randomUUID();
        when(service.create(eq(actor), any())).thenReturn(new WalkResponse(walk));
        mvc.perform(post("/api/walks").header("Authorization", token(actor, "WALKER"))
                .contentType("application/json").content(createBody(45, "2500.50")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/walks/" + walk))
                .andExpect(jsonPath("$.id").value(walk.toString()));
        verify(service).create(eq(actor), any());
    }

    @Test
    void rejectsDurationBelowMinimumAndInvalidPricePrecision() throws Exception {
        String auth = token(UUID.randomUUID(), "WALKER");
        mvc.perform(post("/api/walks").header("Authorization", auth)
                .contentType("application/json").content(createBody(15, "2500.50")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("durationMinutes"));
        mvc.perform(post("/api/walks").header("Authorization", auth)
                .contentType("application/json").content(createBody(45, "2500.005")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("pricePerPet"));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsFractionalCapacityWithoutRoundingIt() throws Exception {
        mvc.perform(post("/api/walks").header("Authorization", token(UUID.randomUUID(), "WALKER"))
                .contentType("application/json").content(createBody(45, "2500.50").replace("\"capacity\":3", "\"capacity\":3.5")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void reportsScheduleConflictsAs409() throws Exception {
        UUID actor = UUID.randomUUID();
        when(service.create(eq(actor), any())).thenThrow(new BusinessRuleViolationException("Horario ocupado"));
        mvc.perform(post("/api/walks").header("Authorization", token(actor, "WALKER"))
                .contentType("application/json").content(createBody(45, "0")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }

    // --- búsqueda -------------------------------------------------------------------------------

    @Test
    void authenticatedUserSeesOnlyPublicWalkData() throws Exception {
        var row = new WalkSearchResponse(UUID.randomUUID(), UUID.randomUUID(), "Palermo",
                LocalDateTime.of(2030, 1, 1, 10, 0), 60, "Plaza", new BigDecimal("2500"), 3, 2,
                new WalkSearchResponse.WalkerSummary(UUID.randomUUID(), "Paseador", new BigDecimal("4.5"), 2));
        when(service.search(any(), any())).thenReturn(new WalkSearchPageResponse(List.of(row), 0, 12, false));
        mvc.perform(get("/api/walks").header("Authorization", token(UUID.randomUUID(), "CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].availableSpots").value(2))
                .andExpect(jsonPath("$.items[0].walker.name").value("Paseador"))
                .andExpect(jsonPath("$.items[0].walker.email").doesNotExist())
                .andExpect(jsonPath("$.items[0].walker.phone").doesNotExist())
                .andExpect(jsonPath("$.items[0].walker.passwordHash").doesNotExist());
    }

    @Test
    void passesTypedFiltersToTheService() throws Exception {
        UUID actor = UUID.randomUUID();
        var zone = UUID.randomUUID();
        var from = LocalDateTime.of(2030, 1, 1, 10, 0);
        var filters = new WalkSearchFilters(zone, from, from.plusHours(2), 1, 5);
        when(service.search(eq(actor), eq(filters))).thenReturn(new WalkSearchPageResponse(List.of(), 1, 5, false));
        mvc.perform(get("/api/walks").header("Authorization", token(actor, "CLIENT"))
                .param("zoneId", zone.toString()).param("startsFrom", from.toString())
                .param("startsTo", from.plusHours(2).toString()).param("page", "1").param("size", "5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1));
        verify(service).search(eq(actor), eq(filters));
    }

    @Test
    void rejectsMalformedFiltersWithTheExistingErrorShape() throws Exception {
        String auth = token(UUID.randomUUID(), "CLIENT");
        for (String[] invalid : List.of(new String[]{"zoneId", "no-es-uuid"},
                new String[]{"startsFrom", "2026-02-31T10:00"}, new String[]{"page", "abc"})) {
            mvc.perform(get("/api/walks").header("Authorization", auth).param(invalid[0], invalid[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value(invalid[0]));
        }
        verifyNoInteractions(service);
    }

    @Test
    void invalidRangeIsBadRequest() throws Exception {
        when(service.search(any(), any())).thenThrow(new InvalidFieldException("startsTo", "Hasta debe ser posterior a Desde."));
        mvc.perform(get("/api/walks").header("Authorization", token(UUID.randomUUID(), "CLIENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("startsTo"));
    }

    // --- agenda (mine) --------------------------------------------------------------------------

    @Test
    void agendaUsesTokenIdentityAndTypedFilters() throws Exception {
        UUID actor = UUID.randomUUID();
        when(service.mine(actor, WalkAgendaPeriod.HISTORY, 1, 5))
                .thenReturn(new WalkAgendaResponse(List.of(), 1, 5, false));
        mvc.perform(get("/api/walks/mine").header("Authorization", token(actor, "WALKER"))
                .param("period", "HISTORY").param("page", "1").param("size", "5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1));
        verify(service).mine(actor, WalkAgendaPeriod.HISTORY, 1, 5);
    }

    @Test
    void clientCannotReadWalkerAgenda() throws Exception {
        UUID actor = UUID.randomUUID();
        when(service.mine(actor, WalkAgendaPeriod.UPCOMING, 0, 12)).thenThrow(new ForbiddenActionException("Sin permiso"));
        mvc.perform(get("/api/walks/mine").header("Authorization", token(actor, "CLIENT")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void malformedPeriodPageAndWalkIdReturnBadRequest() throws Exception {
        String auth = token(UUID.randomUUID(), "WALKER");
        mvc.perform(get("/api/walks/mine").param("period", "INVALID").header("Authorization", auth))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("period"));
        mvc.perform(get("/api/walks/mine").param("page", "abc").header("Authorization", auth)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/walks/no-es-un-uuid").header("Authorization", auth)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    // --- detalle ---------------------------------------------------------------------------------

    @Test
    void detailReturnsDtoWithoutPrivateContactOrParticipants() throws Exception {
        UUID actor = UUID.randomUUID(), id = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2030, 1, 1, 10, 0);
        when(service.getById(actor, id)).thenReturn(new WalkReadResponse(id, UUID.randomUUID(), "Palermo", start,
                start.plusHours(1), 60, "Plaza", BigDecimal.TEN, 3, 1, 2, WalkStatus.PUBLISHED,
                new WalkReadResponse.WalkerSummary(UUID.randomUUID(), "Paseador", BigDecimal.ZERO, 0)));
        mvc.perform(get("/api/walks/" + id).header("Authorization", token(actor, "CLIENT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.bookedSpots").value(1))
                .andExpect(jsonPath("$.walker.email").doesNotExist()).andExpect(jsonPath("$.walker.phone").doesNotExist())
                .andExpect(jsonPath("$.walker.passwordHash").doesNotExist()).andExpect(jsonPath("$.participants").doesNotExist());
    }

    @Test
    void inaccessibleHistoryReturnsNotFound() throws Exception {
        UUID actor = UUID.randomUUID(), id = UUID.randomUUID();
        when(service.getById(actor, id)).thenThrow(new ResourceNotFoundException("Paseo no encontrado."));
        mvc.perform(get("/api/walks/" + id).header("Authorization", token(actor, "CLIENT")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    private String token(UUID id, String role) {
        return "Bearer " + jwt.generateToken(id.toString(), role);
    }

    private String createBody(int duration, String price) {
        return """
                {"zoneId":"%s","startTime":"2030-01-01T11:00","durationMinutes":%d,
                "capacity":3,"pricePerPet":%s,"meetingPoint":"Entrada de la plaza"}
                """.formatted(UUID.randomUUID(), duration, price);
    }
}
