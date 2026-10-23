package com.tpi.paseadores.walk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Los datos se crean solo en una base de prueba y se revierten al terminar cada caso. */
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "TPI_DB_TESTS", matches = "true")
class WalkSearchDatabaseIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired WalkSearchService service;
    private UUID zone;
    private UUID walker;
    private UUID owner;
    private UUID available;
    private LocalDateTime start;

    @BeforeEach
    void fixtures() {
        zone = UUID.randomUUID();
        jdbc.update("INSERT INTO zones(id,name) VALUES (?,?)", zone, "Barrio de prueba " + zone);
        walker = user("WALKER", true);
        owner = user("OWNER", true);
        start = LocalDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")).plusDays(3).withNano(0);
        available = walk(walker, "PUBLISHED", start, 3);
        booking(available, "JOINED");
        booking(available, "CANCELLED");
        UUID full = walk(walker, "PUBLISHED", start.plusHours(1), 1);
        booking(full, "JOINED");
        walk(walker, "CANCELLED", start, 3);
        walk(walker, "COMPLETED", start, 3);
        walk(walker, "PUBLISHED", start.minusDays(4), 3);
        walk(user("WALKER", false), "PUBLISHED", start, 3);
        walk(owner, "PUBLISHED", start, 3);
    }

    @Test
    void excludesUnavailableWalksAndCountsOnlyJoinedBookings() {
        var result = service.search(new WalkSearchFilters(zone, null, null, 0, 12));
        assertThat(result.items()).extracting(WalkSearchResponse::id).containsExactly(available);
        assertThat(result.items().getFirst().availableSpots()).isEqualTo(2);
        assertThat(result.items().getFirst().walker().name()).isEqualTo("Usuario de prueba");
        assertThat(service.search(new WalkSearchFilters(UUID.randomUUID(), null, null, 0, 12)).items()).isEmpty();
    }

    @Test
    void appliesInclusiveFromAndExclusiveTo() {
        assertThat(service.search(new WalkSearchFilters(zone, start, start.plusHours(1), 0, 12)).items())
                .extracting(WalkSearchResponse::id).containsExactly(available);
        assertThat(service.search(new WalkSearchFilters(zone, null, start, 0, 12)).items()).isEmpty();
        assertThat(service.search(new WalkSearchFilters(zone, start.plusSeconds(1), null, 0, 12)).items()).isEmpty();
    }

    @Test
    void pagesResultsInStableStartTimeAndIdOrder() {
        UUID later = walk(walker, "PUBLISHED", start.plusHours(2), 2);
        var first = service.search(new WalkSearchFilters(zone, null, null, 0, 1));
        var second = service.search(new WalkSearchFilters(zone, null, null, 1, 1));
        assertThat(first.items()).extracting(WalkSearchResponse::id).containsExactly(available);
        assertThat(first.hasNext()).isTrue();
        assertThat(second.items()).extracting(WalkSearchResponse::id).containsExactly(later);
        assertThat(second.hasNext()).isFalse();
    }

    private UUID user(String role, boolean active) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id,full_name,email,password_hash,role,zone_id,is_active) VALUES (?,?,?,?,?::user_role,?,?)",
                id, "Usuario de prueba", id + "@example.com", "solo-prueba-no-es-una-clave", role, zone, active);
        return id;
    }

    private UUID walk(UUID walkerId, String status, LocalDateTime time, int capacity) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO walks(id,walker_id,zone_id,start_time,duration_minutes,capacity,price_per_pet,meeting_point,status)
                VALUES (?,?,?,?,60,?,2500,'Entrada de la plaza',?::walk_status)
                """, id, walkerId, zone, Timestamp.valueOf(time), capacity, status);
        return id;
    }

    private void booking(UUID walk, String status) {
        UUID pet = UUID.randomUUID();
        jdbc.update("INSERT INTO pets(id,owner_id,name) VALUES (?,?,'Mascota de prueba')", pet, owner);
        jdbc.update("INSERT INTO walk_pets(walk_id,pet_id,status) VALUES (?,?,?::booking_status)", walk, pet, status);
    }
}
