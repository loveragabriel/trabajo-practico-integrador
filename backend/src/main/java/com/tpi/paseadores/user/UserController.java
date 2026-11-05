package com.tpi.paseadores.user;

import com.tpi.paseadores.user.dto.UserResponse;
import com.tpi.paseadores.user.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// every route works on the authenticated user's own profile; the principal is the user id from the JWT
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public UserResponse getOwnProfile(@AuthenticationPrincipal String userId) {
        return service.getProfile(UUID.fromString(userId));
    }

    @PutMapping("/me")
    public UserResponse updateOwnProfile(@AuthenticationPrincipal String userId,
                                          @Valid @RequestBody UserUpdateRequest request) {
        return service.updateProfile(UUID.fromString(userId), request);
    }
}
