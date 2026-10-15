package com.tpi.paseadores.zone;

import com.tpi.paseadores.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Opt in with a dedicated PostgreSQL database and the application's DB/JWT variables. */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "TPI_DB_TESTS", matches = "true")
class ZoneDatabaseIntegrationTest {
    @Autowired
    private ZoneRepository repository;
    @Autowired
    private JwtService jwt;
    @Autowired
    private MockMvc mvc;

    @Test
    void readsFlywayCatalogThroughTheRealRepositoryAndHttpEndpoint() throws Exception {
        var zones = repository.findAllByOrderByNameAsc();
        assertThat(zones).extracting(Zone::getName).contains("Palermo", "Belgrano");
        var palermo = zones.stream().filter(zone -> zone.getName().equals("Palermo")).findFirst().orElseThrow();
        assertThat(repository.findById(palermo.getId())).isPresent();

        String token = "Bearer " + jwt.generateToken(UUID.randomUUID().toString(), "OWNER");
        mvc.perform(get("/api/zones/" + palermo.getId()).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(palermo.getId().toString()))
                .andExpect(jsonPath("$.name").value("Palermo"));
        mvc.perform(get("/api/zones").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(zones.size()));
    }
}
