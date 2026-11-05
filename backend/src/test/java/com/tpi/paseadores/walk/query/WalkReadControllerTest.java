package com.tpi.paseadores.walk.query;

import com.tpi.paseadores.shared.config.SecurityConfig;
import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.shared.security.JwtAuthenticationFilter;
import com.tpi.paseadores.shared.security.JwtService;
import com.tpi.paseadores.walk.WalkStatus;
import com.tpi.paseadores.walk.query.dto.WalkAgendaPeriod;
import com.tpi.paseadores.walk.query.dto.WalkAgendaResponse;
import com.tpi.paseadores.walk.query.dto.WalkReadResponse;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalkReadController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = "app.jwt.secret=clave-de-prueba-no-usar-en-produccion")
class WalkReadControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean WalkReadService service;

    @Test
    void agendaAndDetailsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/walks/mine")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/walks/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void agendaUsesTokenIdentityAndTypedFilters() throws Exception {
        UUID actor = UUID.randomUUID();
        when(service.agenda(actor, WalkAgendaPeriod.HISTORY, 1, 5))
                .thenReturn(new WalkAgendaResponse(List.of(), 1, 5, false));
        mvc.perform(get("/api/walks/mine").header("Authorization", token(actor, "WALKER"))
                .param("period", "HISTORY").param("page", "1").param("size", "5").param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1));
        verify(service).agenda(actor, WalkAgendaPeriod.HISTORY, 1, 5);
    }

    @Test
    void clientCannotReadWalkerAgenda() throws Exception {
        UUID actor = UUID.randomUUID();
        when(service.agenda(actor, WalkAgendaPeriod.UPCOMING, 0, 12)).thenThrow(new ForbiddenActionException("Sin permiso"));
        mvc.perform(get("/api/walks/mine").header("Authorization", token(actor, "CLIENT")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void malformedPeriodPageAndWalkIdReturnBadRequest() throws Exception {
        String token = token(UUID.randomUUID(), "WALKER");
        mvc.perform(get("/api/walks/mine").param("period", "INVALID").header("Authorization", token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("period"));
        mvc.perform(get("/api/walks/mine").param("page", "abc").header("Authorization", token)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/walks/no-es-un-uuid").header("Authorization", token)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void detailReturnsDtoWithoutPrivateContactOrParticipants() throws Exception {
        UUID actor = UUID.randomUUID(), id = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2030, 1, 1, 10, 0);
        when(service.detail(actor, id)).thenReturn(new WalkReadResponse(id, UUID.randomUUID(), "Palermo", start,
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
        when(service.detail(actor, id)).thenThrow(new ResourceNotFoundException("Paseo no encontrado."));
        mvc.perform(get("/api/walks/" + id).header("Authorization", token(actor, "CLIENT")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    private String token(UUID id, String role) { return "Bearer " + jwt.generateToken(id.toString(), role); }
}
