package com.tpi.paseadores.auth;

import com.tpi.paseadores.auth.dto.LoginRequest;
import com.tpi.paseadores.auth.dto.RegisterRequest;
import com.tpi.paseadores.user.User;
import com.tpi.paseadores.user.UserRepository;
import com.tpi.paseadores.user.UserRole;
import com.tpi.paseadores.zone.ZoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Opt in with a dedicated PostgreSQL database and the application's DB/JWT variables.
 * Exercises the service against a real Postgres to confirm the `user_role` native enum
 * column round-trips correctly through Hibernate's NAMED_ENUM mapping.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "TPI_DB_TESTS", matches = "true")
class AuthDatabaseIntegrationTest {
    @Autowired
    private AuthService service;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ZoneRepository zoneRepository;

    @Test
    void registersAWalkerAndLogsInAgainstTheRealSchema() {
        UUID zoneId = zoneRepository.findAllByOrderByNameAsc().getFirst().getId();
        String email = "walker-" + UUID.randomUUID() + "@example.com";

        var registered = service.register(new RegisterRequest(
                "Integration Walker", email, "password123", null, "WALKER", zoneId, null, null));
        assertThat(registered.user().role()).isEqualTo("WALKER");

        User persisted = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(persisted.getRole()).isEqualTo(UserRole.WALKER);

        var loggedIn = service.login(new LoginRequest(email, "password123"));
        assertThat(loggedIn.user().id()).isEqualTo(registered.user().id());
    }
}
