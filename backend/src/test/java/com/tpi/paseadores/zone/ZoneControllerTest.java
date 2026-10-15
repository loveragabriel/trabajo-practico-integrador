package com.tpi.paseadores.zone;

import com.tpi.paseadores.shared.config.SecurityConfig;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.shared.security.JwtAuthenticationFilter;
import com.tpi.paseadores.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ZoneController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = "app.jwt.secret=test-secret-not-for-production-use")
class ZoneControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtService jwt;
    @MockBean
    private ZoneService service;

    @Test
    void preservesExistingAuthenticationRequirement() throws Exception {
        mvc.perform(get("/api/zones")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        verifyNoInteractions(service);
    }

    @Test
    void returnsOnlyTheZoneDtoForAnOwner() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.findAll()).thenReturn(List.of(new ZoneResponse(id, "Palermo", "Buenos Aires")));
        mvc.perform(get("/api/zones").header("Authorization", token("OWNER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("Palermo"))
                .andExpect(jsonPath("$[0].city").value("Buenos Aires"))
                .andExpect(jsonPath("$[0].createdAt").doesNotExist());
    }

    @Test
    void walkerCanReadZoneDetails() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.findById(id)).thenReturn(new ZoneResponse(id, "Belgrano", "Buenos Aires"));
        mvc.perform(get("/api/zones/" + id).header("Authorization", token("WALKER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Belgrano"));
    }

    @Test
    void emptyCatalogIsAnEmptyArray() throws Exception {
        when(service.findAll()).thenReturn(List.of());
        mvc.perform(get("/api/zones").header("Authorization", token("OWNER")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void unknownUuidUsesExistingNotFoundHandler() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.findById(id)).thenThrow(new ResourceNotFoundException("Zone not found"));
        mvc.perform(get("/api/zones/" + id).header("Authorization", token("OWNER")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Zone not found"));
    }

    @Test
    void malformedUuidReturnsBadRequestInsteadOfInternalError() throws Exception {
        mvc.perform(get("/api/zones/not-a-uuid").header("Authorization", token("OWNER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("id"))
                .andExpect(jsonPath("$.message").value("Invalid parameter format"));
        verifyNoInteractions(service);
    }

    @Test
    void doesNotExposeWriteEndpoints() throws Exception {
        mvc.perform(post("/api/zones").header("Authorization", token("OWNER")))
                .andExpect(status().isMethodNotAllowed()).andExpect(header().string("Allow", "GET"))
                .andExpect(jsonPath("$.status").value(405));
        verifyNoInteractions(service);
    }

    @Test
    void invalidTokenCannotAccessCatalog() throws Exception {
        mvc.perform(get("/api/zones").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    private String token(String role) {
        return "Bearer " + jwt.generateToken(UUID.randomUUID().toString(), role);
    }
}
