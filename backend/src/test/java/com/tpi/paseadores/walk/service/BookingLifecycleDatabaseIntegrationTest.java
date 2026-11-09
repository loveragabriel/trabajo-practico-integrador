package com.tpi.paseadores.walk.service;

import com.tpi.paseadores.review.dto.ReviewCreateRequest;
import com.tpi.paseadores.review.service.ReviewService;
import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.shared.exception.InvalidFieldException;
import com.tpi.paseadores.walk.domain.WalkStatus;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "TPI_DB_TESTS", matches = "true")
class BookingLifecycleDatabaseIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired WalkService walks;
    @Autowired ReviewService reviews;
    @Autowired Clock clock;

    UUID zone, walker, otherWalker, owner, otherOwner, pet, secondPet, foreignPet, walk;

    @BeforeEach
    void setup() {
        zone = UUID.randomUUID();
        jdbc.update("INSERT INTO zones(id,name) VALUES (?,?)", zone, "Zona de reservas " + zone);
        walker = user("WALKER");
        otherWalker = user("WALKER");
        owner = user("CLIENT");
        otherOwner = user("CLIENT");
        pet = pet(owner);
        secondPet = pet(owner);
        foreignPet = pet(otherOwner);
        walk = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO walks(id,walker_id,zone_id,start_time,duration_minutes,capacity,price_per_pet,meeting_point)
                VALUES (?,?,?,?,30,1,2000,'Plaza')
                """, walk, walker, zone, now().plusDays(1));
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM reviews WHERE walk_id=?", walk);
        jdbc.update("DELETE FROM walk_pets WHERE walk_id=?", walk);
        jdbc.update("DELETE FROM walks WHERE id=?", walk);
        jdbc.update("DELETE FROM pets WHERE owner_id IN (?,?)", owner, otherOwner);
        jdbc.update("DELETE FROM users WHERE zone_id=?", zone);
        jdbc.update("DELETE FROM zones WHERE id=?", zone);
    }

    @Test
    void lastSpotCanOnlyBeBookedOnceAndBecomesAvailableAfterCancellation() throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var threads = Executors.newFixedThreadPool(2)) {
            var first = threads.submit(attempt(owner, pet, ready, start));
            var second = threads.submit(attempt(otherOwner, foreignPet, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            boolean ownerWon = first.get(15, TimeUnit.SECONDS);
            assertThat(second.get(15, TimeUnit.SECONDS)).isNotEqualTo(ownerWon);
            UUID winner = ownerWon ? owner : otherOwner;
            UUID winnerPet = ownerWon ? pet : foreignPet;
            assertThat(walks.getById(walker, walk).status()).isEqualTo(WalkStatus.FULL);
            var booking = walks.bookings(winner, walk, 0).getFirst();
            assertThat(booking.canCancel()).isTrue();
            assertThat(walks.bookings(ownerWon ? otherOwner : owner, walk, 0)).isEmpty();

            walks.cancelBooking(winner, walk, booking.id());
            assertThat(walks.getById(walker, walk).availableSpots()).isEqualTo(1);
            assertThat(walks.getById(walker, walk).status()).isEqualTo(WalkStatus.PUBLISHED);
            walks.book(winner, walk, List.of(winnerPet));
            assertThat(walks.bookings(winner, walk, 0)).singleElement()
                    .satisfies(rejoined -> {
                        assertThat(rejoined.id()).isEqualTo(booking.id());
                        assertThat(rejoined.bookingStatus()).isEqualTo("JOINED");
                    });
        }
    }

    @Test
    void batchesAreAtomicAndOnlyTheOwnerCanCancel() {
        assertThatThrownBy(() -> walks.book(owner, walk, List.of(pet, secondPet))).isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> walks.book(owner, walk, List.of(pet, pet))).isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> walks.book(owner, walk, List.of(foreignPet))).isInstanceOf(ForbiddenActionException.class);
        assertThat(walks.bookings(owner, walk, 0)).isEmpty();
        walks.book(owner, walk, List.of(pet));
        var booking = walks.bookings(owner, walk, 0).getFirst();
        assertThatThrownBy(() -> walks.cancelBooking(otherOwner, walk, booking.id())).isInstanceOf(ForbiddenActionException.class);
        assertThatThrownBy(() -> walks.cancelWalk(otherWalker, walk)).isInstanceOf(ForbiddenActionException.class);
        jdbc.update("UPDATE walks SET start_time=? WHERE id=?", now().plusHours(1), walk);
        assertThat(walks.bookings(owner, walk, 0).getFirst().canCancel()).isFalse();
        assertThatThrownBy(() -> walks.cancelBooking(owner, walk, booking.id())).isInstanceOf(BusinessRuleViolationException.class);
        walks.cancelWalk(walker, walk);
        assertThat(walks.bookings(owner, null, 0)).singleElement().satisfies(cancelled -> {
            assertThat(cancelled.bookingStatus()).isEqualTo("CANCELLED");
            assertThat(cancelled.walkStatus()).isEqualTo("CANCELLED");
        });
    }

    @Test
    void reviewsRequireParticipationAndUpdateTheWalkersRating() {
        walks.book(owner, walk, List.of(pet));
        var input = new ReviewCreateRequest(null, 4, "Buen paseo");
        assertThatThrownBy(() -> walks.completeWalk(walker, walk)).isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> reviews.create(owner, walk, input)).isInstanceOf(BusinessRuleViolationException.class);
        jdbc.update("UPDATE walks SET start_time=? WHERE id=?", now().minusMinutes(5), walk);
        walks.startWalk(walker, walk);
        assertThat(walks.getById(walker, walk).status()).isEqualTo(WalkStatus.IN_PROGRESS);
        jdbc.update("UPDATE walks SET start_time=? WHERE id=?", now().minusHours(1), walk);
        walks.completeWalk(walker, walk);
        assertThatThrownBy(() -> reviews.create(otherOwner, walk, input)).isInstanceOf(ForbiddenActionException.class);
        assertThatThrownBy(() -> reviews.create(owner, walk, new ReviewCreateRequest(UUID.randomUUID(), 4, null)))
                .isInstanceOf(InvalidFieldException.class);
        assertThat(reviews.create(owner, walk, input).comment()).isEqualTo("Buen paseo");
        assertThatThrownBy(() -> reviews.create(owner, walk, input)).isInstanceOf(BusinessRuleViolationException.class);
        assertThat(reviews.forWalker(walker, 0)).singleElement().satisfies(r -> assertThat(r.rating()).isEqualTo(4));
        assertThat(jdbc.queryForObject("SELECT rating_avg FROM users WHERE id=?", BigDecimal.class, walker))
                .isEqualByComparingTo("4.0");
        assertThat(jdbc.queryForObject("SELECT review_count FROM users WHERE id=?", Integer.class, walker)).isEqualTo(1);
    }

    private Callable<Boolean> attempt(UUID client, UUID animal, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("No arrancaron las dos solicitudes");
            try {
                walks.book(client, walk, List.of(animal));
                return true;
            } catch (BusinessRuleViolationException full) {
                return false;
            }
        };
    }

    private LocalDateTime now() { return LocalDateTime.now(clock); }

    private UUID user(String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id,full_name,email,password_hash,role,zone_id) VALUES (?,?,?,?,CAST(? AS user_role),?)",
                id, role, id + "@example.com", "hash-de-prueba", role, zone);
        return id;
    }

    private UUID pet(UUID client) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO pets(id,owner_id,name) VALUES (?,?,'Luna')", id, client);
        return id;
    }
}
