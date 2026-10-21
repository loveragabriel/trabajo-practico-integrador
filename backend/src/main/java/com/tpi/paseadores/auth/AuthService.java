package com.tpi.paseadores.auth;

import com.tpi.paseadores.auth.dto.AuthResponse;
import com.tpi.paseadores.auth.dto.LoginRequest;
import com.tpi.paseadores.auth.dto.RegisterRequest;
import com.tpi.paseadores.auth.dto.UserSummary;
import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.shared.exception.InvalidCredentialsException;
import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.shared.security.JwtService;
import com.tpi.paseadores.user.User;
import com.tpi.paseadores.user.UserRole;
import com.tpi.paseadores.user.UserService;
import com.tpi.paseadores.zone.ZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final ZoneService zoneService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        UserRole role = UserRole.fromApiValue(request.role());

        if (userService.existsByEmail(request.email())) {
            throw new BusinessRuleViolationException("Email already registered");
        }
        if (!zoneService.existsById(request.zoneId())) {
            throw new ResourceNotFoundException("Zone not found");
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(role);
        user.setZoneId(request.zoneId());
        if (role == UserRole.WALKER) {
            user.setDefaultPrice(request.defaultPrice());
            user.setDefaultCapacity(request.defaultCapacity());
        }

        user = userService.create(user);
        return new AuthResponse(issueToken(user), UserSummary.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userService.findByEmail(request.email())
                .filter(User::isActive)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return new AuthResponse(issueToken(user), UserSummary.from(user));
    }

    private String issueToken(User user) {
        return jwtService.generateToken(user.getId().toString(), user.getRole().name());
    }
}
