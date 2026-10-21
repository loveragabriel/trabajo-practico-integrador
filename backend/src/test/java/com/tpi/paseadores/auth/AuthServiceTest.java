package com.tpi.paseadores.auth;

import com.tpi.paseadores.auth.dto.AuthResponse;
import com.tpi.paseadores.auth.dto.LoginRequest;
import com.tpi.paseadores.auth.dto.RegisterRequest;
import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.shared.exception.InvalidCredentialsException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.shared.security.JwtService;
import com.tpi.paseadores.user.User;
import com.tpi.paseadores.user.UserRole;
import com.tpi.paseadores.user.UserService;
import com.tpi.paseadores.zone.ZoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserService userService;
    @Mock
    private ZoneService zoneService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService service;

    @BeforeEach
    void setUp() {
        // mocks are injected by MockitoExtension after construction, so the service
        // that wires them up has to be built here, not in a field initializer
        service = new AuthService(userService, zoneService, passwordEncoder, jwtService);
    }

    @Test
    void registersAnOwnerAndIssuesAToken() {
        UUID zoneId = UUID.randomUUID();
        var request = new RegisterRequest("Ana Dueña", "ana@example.com", "password123", null,
                "CLIENT", zoneId, null, null);

        when(userService.existsByEmail("ana@example.com")).thenReturn(false);
        when(zoneService.existsById(zoneId)).thenReturn(true);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userService.create(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(jwtService.generateToken(any(), eq("OWNER"))).thenReturn("jwt-token");

        AuthResponse response = service.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().name()).isEqualTo("Ana Dueña");
        assertThat(response.user().role()).isEqualTo("CLIENT");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).create(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("ana@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getRole()).isEqualTo(UserRole.OWNER);
        assertThat(saved.getZoneId()).isEqualTo(zoneId);
        // an owner never carries walker-only defaults (RN-04 / ck_users_owner_has_no_walker_defaults)
        assertThat(saved.getDefaultPrice()).isNull();
        assertThat(saved.getDefaultCapacity()).isNull();
    }

    @Test
    void registersAWalkerWithOptionalDefaults() {
        UUID zoneId = UUID.randomUUID();
        var request = new RegisterRequest("Beto Paseador", "beto@example.com", "password123", "+54 11 5555-5555",
                "WALKER", zoneId, new BigDecimal("1500.00"), 4);

        when(zoneService.existsById(zoneId)).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userService.create(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(jwtService.generateToken(any(), eq("WALKER"))).thenReturn("jwt-token");

        service.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).create(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo(UserRole.WALKER);
        assertThat(saved.getDefaultPrice()).isEqualByComparingTo("1500.00");
        assertThat(saved.getDefaultCapacity()).isEqualTo(4);
    }

    @Test
    void rejectsRegistrationWithADuplicateEmail() {
        var request = new RegisterRequest("Ana", "ana@example.com", "password123", null,
                "CLIENT", UUID.randomUUID(), null, null);
        when(userService.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Email already registered");
        verifyNoInteractions(zoneService, passwordEncoder, jwtService);
    }

    @Test
    void rejectsRegistrationWithAnUnknownZone() {
        UUID zoneId = UUID.randomUUID();
        var request = new RegisterRequest("Ana", "ana@example.com", "password123", null,
                "CLIENT", zoneId, null, null);
        when(userService.existsByEmail("ana@example.com")).thenReturn(false);
        when(zoneService.existsById(zoneId)).thenReturn(false);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Zone not found");
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void logsInWithCorrectCredentials() {
        User user = activeUser();
        when(userService.findByEmail("ana@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(user.getId().toString(), "OWNER")).thenReturn("jwt-token");

        AuthResponse response = service.login(new LoginRequest("ana@example.com", "password123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().id()).isEqualTo(user.getId());
    }

    @Test
    void rejectsLoginWithAnUnknownEmail() {
        when(userService.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("ghost@example.com", "whatever1")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void rejectsLoginWithAWrongPassword() {
        User user = activeUser();
        when(userService.findByEmail("ana@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-pass", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("ana@example.com", "wrong-pass")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
        verifyNoInteractions(jwtService);
    }

    @Test
    void rejectsLoginForADeactivatedUser() {
        User user = activeUser();
        user.setActive(false);
        when(userService.findByEmail("ana@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new LoginRequest("ana@example.com", "password123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    private static User activeUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName("Ana Dueña");
        user.setEmail("ana@example.com");
        user.setPasswordHash("hashed");
        user.setRole(UserRole.OWNER);
        user.setZoneId(UUID.randomUUID());
        user.setActive(true);
        return user;
    }
}
