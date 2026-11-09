package com.tpi.paseadores.user.controller;

import com.tpi.paseadores.user.dto.WalkerCoverageRequest;
import com.tpi.paseadores.user.dto.WalkerCoverageResponse;
import com.tpi.paseadores.user.dto.WalkerPublicProfileResponse;
import com.tpi.paseadores.user.service.WalkerCoverageService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/walkers")
public class WalkerController {
    private final WalkerCoverageService service;

    public WalkerController(WalkerCoverageService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public WalkerCoverageResponse getOwnCoverage(@AuthenticationPrincipal String userId) {
        return service.getOwnCoverage(UUID.fromString(userId));
    }

    @PutMapping("/me")
    public WalkerCoverageResponse updateOwnCoverage(@AuthenticationPrincipal String userId,
                                                      @Valid @RequestBody WalkerCoverageRequest request) {
        return service.updateOwnCoverage(UUID.fromString(userId), request);
    }

    @GetMapping("/{id}")
    public WalkerPublicProfileResponse getPublicProfile(@PathVariable UUID id) {
        return service.getPublicProfile(id);
    }
}
