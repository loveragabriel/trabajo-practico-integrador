package com.tpi.paseadores.walk;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Opt in with a dedicated PostgreSQL database and the application's DB/JWT variables.
 * Checks the Walk mapping against the real schema (native enum, generated end_time) and that
 * findByIdForUpdate really locks the row (RN-40). Data is committed, because the lock test needs a
 * second connection to see it, and removed after each case.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "TPI_DB_TESTS", matches = "true")
class WalkDatabaseIntegrationTest {
    @Autowired WalkRepository walkRepository;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;

    private UUID zone;
    private UUID walker;
    private UUID walkId;
    private final LocalDateTime start = LocalDateTime.of(2030, 1, 15, 10, 0);

    @BeforeEach
    void fixtures() {
        zone = UUID.randomUUID();
        jdbc.update("INSERT INTO zones(id,name) VALUES (?,?)", zone, "Barrio de prueba " + zone);
        walker = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id,full_name,email,password_hash,role,zone_id) VALUES (?,?,?,?,'WALKER'::user_role,?)",
                walker, "Paseador de prueba", walker + "@example.com", "solo-prueba-no-es-una-clave", zone);
        walkId = tx.execute(s -> walkRepository.saveAndFlush(
                new Walk(walker, zone, start, 45, 4, new BigDecimal("2500.00"), "Entrada de la plaza")).getId());
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM walks WHERE walker_id = ?", walker);
        jdbc.update("DELETE FROM users WHERE id = ?", walker);
        jdbc.update("DELETE FROM zones WHERE id = ?", zone);
    }

    @Test
    void persistsAWalkAndReadsBackTheGeneratedEndTime() {
        Walk walk = walkRepository.findById(walkId).orElseThrow();

        assertThat(walk.getWalkerId()).isEqualTo(walker);
        assertThat(walk.getStatus()).isEqualTo(WalkStatus.PUBLISHED);
        assertThat(walk.getPricePerPet()).isEqualByComparingTo("2500.00");
        assertThat(walk.getEndTime()).isEqualTo(start.plusMinutes(45));
    }

    @Test
    void everyStatusRoundTripsThroughTheNativeEnum() {
        for (WalkStatus status : WalkStatus.values()) {
            jdbc.update("UPDATE walks SET status = ?::walk_status WHERE id = ?", status.name(), walkId);
            assertThat(walkRepository.findById(walkId).orElseThrow().getStatus()).isEqualTo(status);
        }
    }

    @Test
    void findByIdForUpdateBlocksASecondLockOnTheSameWalk() throws Exception {
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var holder = CompletableFuture.runAsync(() -> tx.executeWithoutResult(s -> {
            walkRepository.findByIdForUpdate(walkId).orElseThrow();
            locked.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));

        try {
            assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
            // without RN-40 this second read would return at once and both could take the last spot
            assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
                jdbc.execute("SET LOCAL lock_timeout = '300ms'");
                walkRepository.findByIdForUpdate(walkId);
            })).isInstanceOf(PessimisticLockingFailureException.class);
        } finally {
            release.countDown();
            holder.get(10, TimeUnit.SECONDS);
        }

        // once the first transaction ends, the lock is free again
        Optional<Walk> relocked = tx.execute(s -> walkRepository.findByIdForUpdate(walkId));
        assertThat(relocked).isPresent();
    }
}
