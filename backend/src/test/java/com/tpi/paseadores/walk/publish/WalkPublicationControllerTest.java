package com.tpi.paseadores.walk.publish;

import com.tpi.paseadores.shared.config.SecurityConfig;
import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.shared.security.JwtAuthenticationFilter;
import com.tpi.paseadores.shared.security.JwtService;
import com.tpi.paseadores.walk.publish.dto.WalkPublishResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalkPublicationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = "app.jwt.secret=clave-de-prueba-no-usar-en-produccion")
class WalkPublicationControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockBean WalkPublicationService service;

    @Test
    void requiresAuthenticationForPublication() throws Exception {
        mvc.perform(post("/api/walks").contentType("application/json").content(body(45, "2500.50")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void createsTheWalkForTheTokenActorAndReturnsItsLocation() throws Exception {
        UUID actor = UUID.randomUUID(), walk = UUID.randomUUID();
        when(service.publish(eq(actor), any())).thenReturn(new WalkPublishResponse(walk));
        mvc.perform(post("/api/walks").header("Authorization", token(actor))
                .contentType("application/json").content(body(45, "2500.50")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/walks/" + walk))
                .andExpect(jsonPath("$.id").value(walk.toString()));
        verify(service).publish(eq(actor), any());
    }

    @Test
    void rejectsDurationBelowMinimumAndInvalidPricePrecision() throws Exception {
        String token = token(UUID.randomUUID());
        mvc.perform(post("/api/walks").header("Authorization", token)
                .contentType("application/json").content(body(15, "2500.50")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("durationMinutes"));
        mvc.perform(post("/api/walks").header("Authorization", token)
                .contentType("application/json").content(body(45, "2500.005")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("pricePerPet"));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsFractionalCapacityWithoutRoundingIt() throws Exception {
        mvc.perform(post("/api/walks").header("Authorization", token(UUID.randomUUID()))
                .contentType("application/json").content(body(45, "2500.50").replace("\"capacity\":3", "\"capacity\":3.5")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void reportsScheduleConflictsAs409() throws Exception {
        UUID actor = UUID.randomUUID();
        when(service.publish(eq(actor), any())).thenThrow(new BusinessRuleViolationException("Horario ocupado"));
        mvc.perform(post("/api/walks").header("Authorization", token(actor))
                .contentType("application/json").content(body(45, "0")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }

    private String token(UUID id) {
        return "Bearer " + jwt.generateToken(id.toString(), "WALKER");
    }

    private String body(int duration, String price) {
        return """
                {"zoneId":"%s","startTime":"2030-01-01T11:00","durationMinutes":%d,
                "capacity":3,"pricePerPet":%s,"meetingPoint":"Entrada de la plaza","walkerId":"%s"}
                """.formatted(UUID.randomUUID(), duration, price, UUID.randomUUID());
    }
}
