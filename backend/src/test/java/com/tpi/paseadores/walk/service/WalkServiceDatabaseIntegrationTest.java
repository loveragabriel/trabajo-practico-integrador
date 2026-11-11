package com.tpi.paseadores.walk.service;

import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.walk.domain.WalkStatus;
import com.tpi.paseadores.walk.dto.ClientBookingPeriod;
import com.tpi.paseadores.walk.dto.WalkAgendaPeriod;
import com.tpi.paseadores.walk.dto.WalkCreateRequest;
import com.tpi.paseadores.walk.dto.WalkReadResponse;
import com.tpi.paseadores.walk.dto.WalkSearchFilters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Opt in with a dedicated PostgreSQL database and the application's DB/JWT variables.
 * Cada @Nested cubre uno de los casos de uso de WalkService (crear, agenda/detalle y
 * búsqueda) contra el esquema real, con su propio fixture para no mezclar datos entre ellos.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "TPI_DB_TESTS", matches = "true")
class WalkServiceDatabaseIntegrationTest {

    @Nested
    class Creation {
        @Autowired JdbcTemplate jdbc;
        @Autowired WalkService service;
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
            UUID id = service.create(walker, request(start)).id();
            assertThat(service.mine(walker, WalkAgendaPeriod.UPCOMING, 0, 12).items())
                    .extracting(WalkReadResponse::id).containsExactly(id);
            var detail = service.getById(walker, id);
            assertThat(detail.endTime()).isEqualTo(start.plusMinutes(45));
            assertThat(detail.availableSpots()).isEqualTo(3);
            assertThat(service.search(walker, new WalkSearchFilters(zone, null, null, 0, 12)).items())
                    .extracting(WalkReadResponse::id).containsExactly(id);
        }

        @Test
        void rejectsOverlapsButAllowsAdjacentAndIgnoresCancelled() {
            UUID first = service.create(walker, request(start)).id();
            assertThatThrownBy(() -> service.create(walker, request(start.plusMinutes(30))))
                    .isInstanceOf(BusinessRuleViolationException.class);
            service.create(walker, request(start.plusMinutes(45)));
            jdbc.update("UPDATE walks SET status='CANCELLED' WHERE id=?", first);
            assertThat(service.create(walker, request(start)).id()).isNotEqualTo(first);
        }

        @Test
        void concurrentCreationsOfTheFirstWalkCannotBothSucceed() throws Exception {
            var ready = new CountDownLatch(2);
            var startTogether = new CountDownLatch(1);
            Callable<Boolean> attempt = () -> {
                ready.countDown();
                if (!startTogether.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("No arrancaron las dos solicitudes");
                try {
                    service.create(walker, request(start));
                    return true;
                } catch (BusinessRuleViolationException conflict) {
                    return false;
                }
            };
            try (var threads = Executors.newFixedThreadPool(2)) {
                var firstAttempt = threads.submit(attempt);
                var secondAttempt = threads.submit(attempt);
                boolean bothReady = ready.await(5, TimeUnit.SECONDS);
                startTogether.countDown();
                assertThat(bothReady).isTrue();
                assertThat(firstAttempt.get(15, TimeUnit.SECONDS)).isNotEqualTo(secondAttempt.get(15, TimeUnit.SECONDS));
            }
            assertThat(jdbc.queryForObject("SELECT count(*) FROM walks WHERE walker_id=?", Integer.class, walker))
                    .isEqualTo(1);
        }

        private WalkCreateRequest request(LocalDateTime time) {
            return new WalkCreateRequest(zone, time, 45, 3, new BigDecimal("2500.50"), "Entrada de la plaza");
        }
    }

    @Nested
    @Transactional
    class AgendaAndDetail {
        @Autowired JdbcTemplate jdbc;
        @Autowired WalkService service;
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
            var upcoming = service.mine(walker, WalkAgendaPeriod.UPCOMING, 0, 12);
            assertThat(upcoming.items()).extracting(WalkReadResponse::id).containsExactly(future, full);
            assertThat(upcoming.items().getFirst().bookedSpots()).isEqualTo(1);
            assertThat(upcoming.items().getFirst().availableSpots()).isEqualTo(2);
            assertThat(service.mine(walker, WalkAgendaPeriod.HISTORY, 0, 12).items())
                    .extracting(WalkReadResponse::id).containsExactly(cancelled, past);
            var first = service.mine(walker, WalkAgendaPeriod.UPCOMING, 0, 1);
            var second = service.mine(walker, WalkAgendaPeriod.UPCOMING, 1, 1);
            assertThat(first.items()).extracting(WalkReadResponse::id).containsExactly(future);
            assertThat(first.hasNext()).isTrue();
            assertThat(second.items()).extracting(WalkReadResponse::id).containsExactly(full);
            assertThat(second.hasNext()).isFalse();
        }

        @Test
        void historyOnlyBelongsToItsWalkerOrAnActualParticipant() {
            assertThat(service.getById(walker, past).id()).isEqualTo(past);
            assertThat(service.getById(client, past).id()).isEqualTo(past);
            assertThatThrownBy(() -> service.getById(otherWalker, past)).isInstanceOf(ResourceNotFoundException.class);
            assertThatThrownBy(() -> service.getById(otherClient, past)).isInstanceOf(ResourceNotFoundException.class);
            assertThat(service.getById(otherClient, full).availableSpots()).isZero();
            assertThatThrownBy(() -> service.mine(client, WalkAgendaPeriod.UPCOMING, 0, 12))
                    .isInstanceOf(ForbiddenActionException.class);
        }

        @Test
        void inactiveAccountsAndWalkersAreNotExposed() {
            jdbc.update("UPDATE users SET is_active=false WHERE id=?", walker);
            assertThatThrownBy(() -> service.getById(client, future)).isInstanceOf(ResourceNotFoundException.class);
            assertThatThrownBy(() -> service.mine(walker, WalkAgendaPeriod.UPCOMING, 0, 12))
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

    @Nested
    @Transactional
    class Search {
        @Autowired JdbcTemplate jdbc;
        @Autowired WalkService service;
        private final UUID actor = UUID.randomUUID();
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
            owner = user("CLIENT", true);
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
            var result = service.search(actor, new WalkSearchFilters(zone, null, null, 0, 12));
            assertThat(result.items()).extracting(WalkReadResponse::id).containsExactly(available);
            assertThat(result.items().getFirst().availableSpots()).isEqualTo(2);
            assertThat(result.items().getFirst().walker().name()).isEqualTo("Usuario de prueba");
            assertThat(service.search(actor, new WalkSearchFilters(UUID.randomUUID(), null, null, 0, 12)).items()).isEmpty();
        }

        @Test
        void appliesInclusiveFromAndExclusiveTo() {
            assertThat(service.search(actor, new WalkSearchFilters(zone, start, start.plusHours(1), 0, 12)).items())
                    .extracting(WalkReadResponse::id).containsExactly(available);
            assertThat(service.search(actor, new WalkSearchFilters(zone, null, start, 0, 12)).items()).isEmpty();
            assertThat(service.search(actor, new WalkSearchFilters(zone, start.plusSeconds(1), null, 0, 12)).items()).isEmpty();
        }

        @Test
        void pagesResultsInStableStartTimeAndIdOrder() {
            UUID later = walk(walker, "PUBLISHED", start.plusHours(2), 2);
            var first = service.search(actor, new WalkSearchFilters(zone, null, null, 0, 1));
            var second = service.search(actor, new WalkSearchFilters(zone, null, null, 1, 1));
            assertThat(first.items()).extracting(WalkReadResponse::id).containsExactly(available);
            assertThat(first.hasNext()).isTrue();
            assertThat(second.items()).extracting(WalkReadResponse::id).containsExactly(later);
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

    // Sin @Transactional a propósito: la prueba de concurrencia necesita que los fixtures ya estén
    // confirmados antes de que arranquen los hilos, y que cada hilo use su propia transacción real
    // (igual que Creation.concurrentCreationsOfTheFirstWalkCannotBothSucceed).
    @Nested
    class Bookings {
        @Autowired JdbcTemplate jdbc;
        @Autowired WalkService service;
        @Autowired BookingService bookingService;
        @Autowired Clock clock;

        private UUID zone, walker, client, otherClient;
        private LocalDateTime start;

        @BeforeEach
        void fixtures() {
            zone = UUID.randomUUID();
            jdbc.update("INSERT INTO zones(id,name) VALUES (?,?)", zone, "Barrio de reservas " + zone);
            walker = user("WALKER");
            client = user("CLIENT");
            otherClient = user("CLIENT");
            start = LocalDateTime.now(clock).plusDays(2).withSecond(0).withNano(0);
        }

        @AfterEach
        void cleanup() {
            jdbc.update("DELETE FROM walk_pets WHERE walk_id IN (SELECT id FROM walks WHERE walker_id=?)", walker);
            jdbc.update("DELETE FROM walks WHERE walker_id=?", walker);
            jdbc.update("DELETE FROM pets WHERE owner_id IN (?,?)", client, otherClient);
            jdbc.update("DELETE FROM users WHERE id IN (?,?,?)", walker, client, otherClient);
            jdbc.update("DELETE FROM zones WHERE id=?", zone);
        }

        @Test
        void joinFillsTheWalkAndCancelBookingReopensIt() {
            UUID walkId = walk(1);
            UUID pet = pet(client);

            bookingService.join(client, walkId, pet);
            assertThat(status(walkId)).isEqualTo("FULL");

            bookingService.cancelBooking(client, walkId, pet);
            assertThat(status(walkId)).isEqualTo("PUBLISHED");
            assertThat(bookingStatus(walkId, pet)).isEqualTo("CANCELLED");
        }

        @Test
        void rejoiningACancelledBookingReusesTheSameRow() {
            UUID walkId = walk(3);
            UUID pet = pet(client);
            var first = bookingService.join(client, walkId, pet);
            bookingService.cancelBooking(client, walkId, pet);
            var second = bookingService.join(client, walkId, pet);

            assertThat(second.id()).isEqualTo(first.id());
            assertThat(jdbc.queryForObject(
                    "SELECT count(*) FROM walk_pets WHERE walk_id=? AND pet_id=?", Integer.class, walkId, pet))
                    .isEqualTo(1);
        }

        @Test
        void concurrentJoinsForTheLastSpotCannotBothSucceed() throws Exception {
            UUID walkId = walk(1);
            UUID petA = pet(client);
            UUID petB = pet(otherClient);
            var ready = new CountDownLatch(2);
            var startTogether = new CountDownLatch(1);
            Callable<Boolean> attemptA = () -> attempt(ready, startTogether, client, walkId, petA);
            Callable<Boolean> attemptB = () -> attempt(ready, startTogether, otherClient, walkId, petB);
            try (var threads = Executors.newFixedThreadPool(2)) {
                var first = threads.submit(attemptA);
                var second = threads.submit(attemptB);
                boolean bothReady = ready.await(5, TimeUnit.SECONDS);
                startTogether.countDown();
                assertThat(bothReady).isTrue();
                assertThat(first.get(15, TimeUnit.SECONDS)).isNotEqualTo(second.get(15, TimeUnit.SECONDS));
            }
            assertThat(jdbc.queryForObject(
                    "SELECT count(*) FROM walk_pets WHERE walk_id=? AND status='JOINED'", Integer.class, walkId))
                    .isEqualTo(1);
        }

        @Test
        void cancellingAWalkCancelsEveryActiveBookingInTheSameCall() {
            UUID walkId = walk(3);
            UUID petA = pet(client);
            UUID petB = pet(otherClient);
            bookingService.join(client, walkId, petA);
            bookingService.join(otherClient, walkId, petB);

            service.updateStatus(walker, walkId, WalkStatus.CANCELLED);

            assertThat(status(walkId)).isEqualTo("CANCELLED");
            assertThat(bookingStatus(walkId, petA)).isEqualTo("CANCELLED");
            assertThat(bookingStatus(walkId, petB)).isEqualTo("CANCELLED");
        }

        @Test
        void cannotStartAWalkWithoutAnyBooking() {
            UUID walkId = walk(3);
            assertThatThrownBy(() -> service.updateStatus(walker, walkId, WalkStatus.IN_PROGRESS))
                    .isInstanceOf(BusinessRuleViolationException.class);
            assertThat(status(walkId)).isEqualTo("PUBLISHED");
        }

        @Test
        void clientBookingMovesThroughTabsEvenBeforeScheduledEnd() {
            UUID walkId = walk(1);
            UUID pet = pet(client);
            bookingService.join(client, walkId, pet);

            assertThat(bookingService.myBookings(client, ClientBookingPeriod.UPCOMING, 0, 12).items())
                    .singleElement().satisfies(booking -> assertThat(booking.walkId()).isEqualTo(walkId));
            assertThat(bookingService.myBookings(client, ClientBookingPeriod.IN_PROGRESS, 0, 12).items()).isEmpty();

            service.updateStatus(walker, walkId, WalkStatus.IN_PROGRESS);
            assertThat(bookingService.myBookings(client, ClientBookingPeriod.UPCOMING, 0, 12).items()).isEmpty();
            assertThat(bookingService.myBookings(client, ClientBookingPeriod.IN_PROGRESS, 0, 12).items())
                    .singleElement().satisfies(booking -> assertThat(booking.walkId()).isEqualTo(walkId));
            assertThat(bookingService.myBookings(otherClient, ClientBookingPeriod.IN_PROGRESS, 0, 12).items()).isEmpty();

            service.updateStatus(walker, walkId, WalkStatus.COMPLETED);
            assertThat(bookingService.myBookings(client, ClientBookingPeriod.IN_PROGRESS, 0, 12).items()).isEmpty();
            assertThat(bookingService.myBookings(client, ClientBookingPeriod.HISTORY, 0, 12).items())
                    .singleElement().satisfies(booking -> assertThat(booking.walkStatus()).isEqualTo(WalkStatus.COMPLETED));
        }

        private boolean attempt(CountDownLatch ready, CountDownLatch startTogether, UUID actor, UUID walkId, UUID petId)
                throws InterruptedException {
            ready.countDown();
            if (!startTogether.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("No arrancaron las dos solicitudes");
            try {
                bookingService.join(actor, walkId, petId);
                return true;
            } catch (BusinessRuleViolationException conflict) {
                return false;
            }
        }

        private UUID user(String role) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO users(id,full_name,email,password_hash,role,zone_id) VALUES (?,?,?,?,?::user_role,?)",
                    id, "Usuario de prueba", id + "@example.com", "hash-solo-de-prueba", role, zone);
            return id;
        }

        private UUID walk(int capacity) {
            UUID id = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO walks(id,walker_id,zone_id,start_time,duration_minutes,capacity,price_per_pet,meeting_point,status)
                    VALUES (?,?,?,?,60,?,2500,'Entrada de la plaza','PUBLISHED')
                    """, id, walker, zone, Timestamp.valueOf(start), capacity);
            return id;
        }

        private UUID pet(UUID ownerId) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO pets(id,owner_id,name) VALUES (?,?,'Mascota de prueba')", id, ownerId);
            return id;
        }

        private String status(UUID walkId) {
            return jdbc.queryForObject("SELECT status FROM walks WHERE id=?", String.class, walkId);
        }

        private String bookingStatus(UUID walkId, UUID petId) {
            return jdbc.queryForObject(
                    "SELECT status FROM walk_pets WHERE walk_id=? AND pet_id=?", String.class, walkId, petId);
        }
    }
}
