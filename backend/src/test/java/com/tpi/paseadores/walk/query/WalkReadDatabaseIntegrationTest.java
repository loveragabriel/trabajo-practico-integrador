package com.tpi.paseadores.walk.query;

import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.walk.query.dto.WalkAgendaPeriod;
import com.tpi.paseadores.walk.query.dto.WalkReadResponse;
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

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "TPI_DB_TESTS", matches = "true")
class WalkReadDatabaseIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired WalkReadService service;
    private UUID zone, walker, otherWalker, client, otherClient, future, full, past, cancelled;
    private LocalDateTime now;

    @BeforeEach
    void fixtures() {
        zone = UUID.randomUUID();
        jdbc.update("INSERT INTO zones(id,name) VALUES (?,?)", zone, "Barrio de prueba " + zone);
        walker = user("WALKER"); otherWalker = user("WALKER");
        client = user("CLIENT"); otherClient = user("CLIENT");
        now = LocalDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")).withNano(0);
        future = walk(walker, "PUBLISHED", now.plusDays(2), 3);
        full = walk(walker, "FULL", now.plusDays(3), 1);
        past = walk(walker, "COMPLETED", now.minusDays(1), 3);
        cancelled = walk(walker, "CANCELLED", now.plusDays(4), 3);
        walk(otherWalker, "PUBLISHED", now.plusDays(2), 3);
        booking(future, client, "JOINED"); booking(future, otherClient, "CANCELLED");
        booking(full, client, "JOINED"); booking(past, client, "JOINED");
    }

    @Test
    void agendaSeparatesPeriodsAndNeverIncludesAnotherWalker() {
        var upcoming = service.agenda(walker, WalkAgendaPeriod.UPCOMING, 0, 12);
        assertThat(upcoming.items()).extracting(WalkReadResponse::id).containsExactly(future, full);
        assertThat(upcoming.items().getFirst().bookedSpots()).isEqualTo(1);
        assertThat(upcoming.items().getFirst().availableSpots()).isEqualTo(2);
        assertThat(service.agenda(walker, WalkAgendaPeriod.HISTORY, 0, 12).items())
                .extracting(WalkReadResponse::id).containsExactly(cancelled, past);
        var first = service.agenda(walker, WalkAgendaPeriod.UPCOMING, 0, 1);
        var second = service.agenda(walker, WalkAgendaPeriod.UPCOMING, 1, 1);
        assertThat(first.items()).extracting(WalkReadResponse::id).containsExactly(future);
        assertThat(first.hasNext()).isTrue();
        assertThat(second.items()).extracting(WalkReadResponse::id).containsExactly(full);
        assertThat(second.hasNext()).isFalse();
    }

    @Test
    void historyOnlyBelongsToItsWalkerOrAnActualParticipant() {
        assertThat(service.detail(walker, past).id()).isEqualTo(past);
        assertThat(service.detail(client, past).id()).isEqualTo(past);
        assertThatThrownBy(() -> service.detail(otherWalker, past)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.detail(otherClient, past)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(service.detail(otherClient, full).availableSpots()).isZero();
        assertThatThrownBy(() -> service.agenda(client, WalkAgendaPeriod.UPCOMING, 0, 12))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void inactiveAccountsAndWalkersAreNotExposed() {
        jdbc.update("UPDATE users SET is_active=false WHERE id=?", walker);
        assertThatThrownBy(() -> service.detail(client, future)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.agenda(walker, WalkAgendaPeriod.UPCOMING, 0, 12))
                .isInstanceOf(ForbiddenActionException.class);
    }

    private UUID user(String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id,full_name,email,password_hash,role,zone_id) VALUES (?,?,?,?,?::user_role,?)",
                id, "Usuario de prueba", id + "@example.com", "hash-solo-de-prueba", role, zone);
        return id;
    }

    private UUID walk(UUID owner, String status, LocalDateTime start, int capacity) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO walks(id,walker_id,zone_id,start_time,duration_minutes,capacity,price_per_pet,meeting_point,status)
                VALUES (?,?,?,?,60,?,2500,'Entrada de la plaza',?::walk_status)
                """, id, owner, zone, Timestamp.valueOf(start), capacity, status);
        return id;
    }

    private void booking(UUID walk, UUID owner, String status) {
        UUID pet = UUID.randomUUID();
        jdbc.update("INSERT INTO pets(id,owner_id,name) VALUES (?,?,'Mascota de prueba')", pet, owner);
        jdbc.update("INSERT INTO walk_pets(walk_id,pet_id,status) VALUES (?,?,?::booking_status)", walk, pet, status);
    }
}
