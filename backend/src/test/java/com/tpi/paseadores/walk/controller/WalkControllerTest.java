package com.tpi.paseadores.walk.controller;

import com.tpi.paseadores.shared.config.SecurityConfig;
import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.InvalidFieldException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.shared.security.JwtAuthenticationFilter;
import com.tpi.paseadores.shared.security.JwtService;
import com.tpi.paseadores.shared.dto.PageResponse;
import com.tpi.paseadores.pet.domain.PetSize;
import com.tpi.paseadores.walk.domain.BookingStatus;
import com.tpi.paseadores.walk.domain.WalkStatus;
import com.tpi.paseadores.walk.dto.BookingResponse;
import com.tpi.paseadores.walk.dto.JoinedPetResponse;
import com.tpi.paseadores.walk.dto.WalkAgendaPeriod;
import com.tpi.paseadores.walk.dto.WalkReadResponse;
import com.tpi.paseadores.walk.dto.WalkSearchFilters;
import com.tpi.paseadores.walk.service.BookingService;
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
    @MockBean BookingService bookingService;

    @Test
    void everyRouteRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/walks").contentType("application/json").content(createBody(45, "2500.50")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/walks")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/walks/mine")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/walks/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/walks/" + UUID.randomUUID() + "/pets")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service, bookingService);
    }

    // --- crear ---------------------------------------------------------------------------------

    @Test
    void createsTheWalkForTheTokenActorAndReturnsItsLocation() throws Exception {
        UUID actor = UUID.randomUUID(), walk = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2030, 1, 1, 11, 0);
        when(service.create(eq(actor), any())).thenReturn(new WalkReadResponse(walk, UUID.randomUUID(), "Palermo",
                start, start.plusMinutes(45), 45, "Entrada de la plaza", new BigDecimal("2500.50"), 3, 0, 3,
                WalkStatus.PUBLISHED, new WalkReadResponse.WalkerSummary(actor, "Paseador", BigDecimal.ZERO, 0)));
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
        LocalDateTime rowStart = LocalDateTime.of(2030, 1, 1, 10, 0);
        var row = new WalkReadResponse(UUID.randomUUID(), UUID.randomUUID(), "Palermo",
                rowStart, rowStart.plusHours(1), 60, "Plaza", new BigDecimal("2500"), 3, 1, 2, WalkStatus.PUBLISHED,
                new WalkReadResponse.WalkerSummary(UUID.randomUUID(), "Paseador", new BigDecimal("4.5"), 2));
        when(service.search(any(), any())).thenReturn(new PageResponse<>(List.of(row), 0, 12, false));
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
        when(service.search(eq(actor), eq(filters))).thenReturn(new PageResponse<>(List.of(), 1, 5, false));
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
                .thenReturn(new PageResponse<>(List.of(), 1, 5, false));
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

    // --- mascotas inscriptas (joinedPets) -------------------------------------------------------

    @Test
    void joinedPetsReturnsTheListForTheTokenActor() throws Exception {
        UUID actor = UUID.randomUUID(), id = UUID.randomUUID(), petId = UUID.randomUUID();
        when(service.getJoinedPets(actor, id)).thenReturn(List.of(new JoinedPetResponse(petId, "Rex", "Labrador",
                PetSize.MEDIUM, "Tira de la correa", new JoinedPetResponse.OwnerSummary("Dueño", "+54 11 1234-5678"))));
        mvc.perform(get("/api/walks/" + id + "/pets").header("Authorization", token(actor, "WALKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].petName").value("Rex"))
                .andExpect(jsonPath("$[0].owner.name").value("Dueño"))
                .andExpect(jsonPath("$[0].owner.phone").value("+54 11 1234-5678"));
        verify(service).getJoinedPets(actor, id);
    }

    @Test
    void joinedPetsForbidsNonOwningActors() throws Exception {
        UUID actor = UUID.randomUUID(), id = UUID.randomUUID();
        when(service.getJoinedPets(actor, id))
                .thenThrow(new ForbiddenActionException("No tenés permiso para consultar las mascotas de este paseo."));
        mvc.perform(get("/api/walks/" + id + "/pets").header("Authorization", token(actor, "WALKER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void joinedPetsPropagatesNotFound() throws Exception {
        UUID actor = UUID.randomUUID(), id = UUID.randomUUID();
        when(service.getJoinedPets(actor, id)).thenThrow(new ResourceNotFoundException("Paseo no encontrado."));
        mvc.perform(get("/api/walks/" + id + "/pets").header("Authorization", token(actor, "WALKER")))
                .andExpect(status().isNotFound());
    }

    // --- join ------------------------------------------------------------------------------------

    @Test
    void joinCreatesTheBookingForTheTokenActor() throws Exception {
        UUID actor = UUID.randomUUID(), id = UUID.randomUUID(), petId = UUID.randomUUID(), bookingId = UUID.randomUUID();
        when(bookingService.join(eq(actor), eq(id), eq(petId))).thenReturn(
                new BookingResponse(bookingId, petId, "Rex", BookingStatus.JOINED, LocalDateTime.of(2030, 1, 1, 10, 0)));
        mvc.perform(post("/api/walks/" + id + "/join/" + petId).header("Authorization", token(actor, "CLIENT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.petName").value("Rex"))
                .andExpect(jsonPath("$.status").value("JOINED"));
    }

    @Test
    void joinRejectsAMalformedPetIdWithBadRequest() throws Exception {
        mvc.perform(post("/api/walks/" + UUID.randomUUID() + "/join/no-es-un-uuid")
                .header("Authorization", token(UUID.randomUUID(), "CLIENT")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(bookingService);
    }

    @Test
    void joinReportsBusinessRuleViolationsAs409() throws Exception {
        UUID actor = UUID.randomUUID(), id = UUID.randomUUID(), petId = UUID.randomUUID();
        when(bookingService.join(eq(actor), eq(id), eq(petId))).thenThrow(new BusinessRuleViolationException("Ya no quedan lugares."));
        mvc.perform(post("/api/walks/" + id + "/join/" + petId).header("Authorization", token(actor, "CLIENT")))
                .andExpect(status().isConflict());
    }

    // --- cancelBooking -----------------------------------------------------------------------------

    @Test
    void cancelBookingReturnsNoContentAndUsesTheTokenActor() throws Exception {
        UUID actor = UUID.randomUUID(), id = UUID.randomUUID(), petId = UUID.randomUUID();
        mvc.perform(delete("/api/walks/" + id + "/join/" + petId).header("Authorization", token(actor, "CLIENT")))
                .andExpect(status().isNoContent());
        verify(bookingService).cancelBooking(actor, id, petId);
    }

    @Test
    void cancelBookingPropagatesNotFound() throws Exception {
        UUID actor = UUID.randomUUID(), id = UUID.randomUUID(), petId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("No encontramos esa reserva."))
                .when(bookingService).cancelBooking(actor, id, petId);
        mvc.perform(delete("/api/walks/" + id + "/join/" + petId).header("Authorization", token(actor, "CLIENT")))
                .andExpect(status().isNotFound());
    }

    // --- updateStatus ------------------------------------------------------------------------------

    @Test
    void updateStatusReturnsNoContentAndUsesTheTokenActor() throws Exception {
        UUID actor = UUID.randomUUID(), id = UUID.randomUUID();
        mvc.perform(patch("/api/walks/" + id + "/status").header("Authorization", token(actor, "WALKER"))
                .contentType("application/json").content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isNoContent());
        verify(service).updateStatus(actor, id, WalkStatus.CANCELLED);
    }

    @Test
    void updateStatusRejectsAMissingStatusWithBadRequest() throws Exception {
        mvc.perform(patch("/api/walks/" + UUID.randomUUID() + "/status").header("Authorization", token(UUID.randomUUID(), "WALKER"))
                .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void updateStatusReportsBusinessRuleViolationsAs409() throws Exception {
        UUID actor = UUID.randomUUID(), id = UUID.randomUUID();
        doThrow(new BusinessRuleViolationException("No se puede iniciar un paseo sin mascotas inscriptas."))
                .when(service).updateStatus(actor, id, WalkStatus.IN_PROGRESS);
        mvc.perform(patch("/api/walks/" + id + "/status").header("Authorization", token(actor, "WALKER"))
                .contentType("application/json").content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateStatusReportsForbiddenForNonOwningWalkers() throws Exception {
        UUID actor = UUID.randomUUID(), id = UUID.randomUUID();
        doThrow(new ForbiddenActionException("No tenés permiso para modificar este paseo."))
                .when(service).updateStatus(actor, id, WalkStatus.CANCELLED);
        mvc.perform(patch("/api/walks/" + id + "/status").header("Authorization", token(actor, "WALKER"))
                .contentType("application/json").content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isForbidden());
    }

    // --- bookings/mine ------------------------------------------------------------------------------

    @Test
    void myBookingsUsesTokenIdentityAndTypedFilters() throws Exception {
        UUID actor = UUID.randomUUID();
        when(bookingService.myBookings(actor, WalkAgendaPeriod.HISTORY, 1, 5))
                .thenReturn(new PageResponse<>(List.of(), 1, 5, false));
        mvc.perform(get("/api/walks/bookings/mine").header("Authorization", token(actor, "CLIENT"))
                .param("period", "HISTORY").param("page", "1").param("size", "5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1));
        verify(bookingService).myBookings(actor, WalkAgendaPeriod.HISTORY, 1, 5);
    }

    @Test
    void walkersCannotListClientBookings() throws Exception {
        UUID actor = UUID.randomUUID();
        when(bookingService.myBookings(actor, WalkAgendaPeriod.UPCOMING, 0, 12)).thenThrow(new ForbiddenActionException("Sin permiso"));
        mvc.perform(get("/api/walks/bookings/mine").header("Authorization", token(actor, "WALKER")))
                .andExpect(status().isForbidden());
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
