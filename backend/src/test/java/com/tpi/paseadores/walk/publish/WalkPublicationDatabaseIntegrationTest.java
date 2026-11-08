package com.tpi.paseadores.walk.publish;

import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.walk.publish.dto.WalkPublishRequest;
import com.tpi.paseadores.walk.query.WalkReadService;
import com.tpi.paseadores.walk.query.dto.WalkAgendaPeriod;
import com.tpi.paseadores.walk.query.dto.WalkReadResponse;
import com.tpi.paseadores.walk.search.WalkSearchService;
import com.tpi.paseadores.walk.search.dto.WalkSearchFilters;
import com.tpi.paseadores.walk.search.dto.WalkSearchResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "TPI_DB_TESTS", matches = "true")
class WalkPublicationDatabaseIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired WalkPublicationService publication;
    @Autowired WalkReadService reads;
    @Autowired WalkSearchService search;
    @Autowired Clock clock;

    private UUID zone;
    private UUID walker;
    private LocalDateTime start;

    @BeforeEach
    void fixtures() {
        zone = UUID.randomUUID();
        walker = UUID.randomUUID();
        start = LocalDateTime.now(clock).plusDays(2).withSecond(0).withNano(0);
        jdbc.update("INSERT INTO zones(id,name) VALUES (?,?)", zone, "Barrio de publicación " + zone);
        jdbc.update("INSERT INTO users(id,full_name,email,password_hash,role,zone_id) VALUES (?,?,?,?,'WALKER',?)",
                walker, "Paseador de prueba", walker + "@example.com", "hash-solo-de-prueba", zone);
        jdbc.update("INSERT INTO walker_zones(walker_id,zone_id) VALUES (?,?)", walker, zone);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM walks WHERE walker_id=?", walker);
        jdbc.update("DELETE FROM walker_zones WHERE walker_id=?", walker);
        jdbc.update("DELETE FROM users WHERE id=?", walker);
        jdbc.update("DELETE FROM zones WHERE id=?", zone);
    }

    @Test
    void persistsAndAppearsInAgendaDetailAndPublicSearch() {
        UUID id = publication.publish(walker, request(start)).id();
        assertThat(reads.agenda(walker, WalkAgendaPeriod.UPCOMING, 0, 12).items())
                .extracting(WalkReadResponse::id).containsExactly(id);
        var detail = reads.detail(walker, id);
        assertThat(detail.endTime()).isEqualTo(start.plusMinutes(45));
        assertThat(detail.availableSpots()).isEqualTo(3);
        assertThat(search.search(new WalkSearchFilters(zone, null, null, 0, 12)).items())
                .extracting(WalkSearchResponse::id).containsExactly(id);
    }

    @Test
    void rejectsOverlapsButAllowsAdjacentAndIgnoresCancelled() {
        UUID first = publication.publish(walker, request(start)).id();
        assertThatThrownBy(() -> publication.publish(walker, request(start.plusMinutes(30))))
                .isInstanceOf(BusinessRuleViolationException.class);
        publication.publish(walker, request(start.plusMinutes(45)));
        jdbc.update("UPDATE walks SET status='CANCELLED' WHERE id=?", first);
        assertThat(publication.publish(walker, request(start)).id()).isNotEqualTo(first);
    }

    @Test
    void concurrentPublicationsOfTheFirstWalkCannotBothSucceed() throws Exception {
        var ready = new CountDownLatch(2);
        var startTogether = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> {
            ready.countDown();
            if (!startTogether.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("No arrancaron las dos solicitudes");
            try {
                publication.publish(walker, request(start));
                return true;
            } catch (BusinessRuleViolationException conflict) {
                return false;
            }
        };
        try (var threads = Executors.newFixedThreadPool(2)) {
            var first = threads.submit(attempt);
            var second = threads.submit(attempt);
            boolean bothReady = ready.await(5, TimeUnit.SECONDS);
            startTogether.countDown();
            assertThat(bothReady).isTrue();
            assertThat(first.get(15, TimeUnit.SECONDS)).isNotEqualTo(second.get(15, TimeUnit.SECONDS));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM walks WHERE walker_id=?", Integer.class, walker))
                .isEqualTo(1);
    }

    private WalkPublishRequest request(LocalDateTime time) {
        return new WalkPublishRequest(zone, time, 45, 3, new BigDecimal("2500.50"), "Entrada de la plaza");
    }
}
