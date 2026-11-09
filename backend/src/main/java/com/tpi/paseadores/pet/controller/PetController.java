package com.tpi.paseadores.pet.controller;

import com.tpi.paseadores.pet.dto.PetRequest;
import com.tpi.paseadores.pet.dto.PetResponse;
import com.tpi.paseadores.pet.service.PetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// every route works on the authenticated user's pets; the principal is the user id from the JWT
@RestController
@RequestMapping("/api/pets")
public class PetController {
    private final PetService service;

    public PetController(PetService service) {
        this.service = service;
    }

    @GetMapping
    public List<PetResponse> listAll(@AuthenticationPrincipal String userId) {
        return service.getAll(UUID.fromString(userId));
    }

    @GetMapping("/{id}")
    public PetResponse getById(@AuthenticationPrincipal String userId, @PathVariable UUID id) {
        return service.getById(UUID.fromString(userId), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PetResponse create(@AuthenticationPrincipal String userId, @Valid @RequestBody PetRequest request) {
        return service.create(UUID.fromString(userId), request);
    }

    @PutMapping("/{id}")
    public PetResponse update(@AuthenticationPrincipal String userId, @PathVariable UUID id,
                              @Valid @RequestBody PetRequest request) {
        return service.update(UUID.fromString(userId), id, request);
    }
}
