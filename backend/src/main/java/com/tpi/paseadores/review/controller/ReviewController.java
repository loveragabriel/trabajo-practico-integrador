package com.tpi.paseadores.review.controller;

import com.tpi.paseadores.review.dto.ReviewCreateRequest;
import com.tpi.paseadores.review.dto.ReviewResponse;
import com.tpi.paseadores.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/walks/{walkId}/reviews")
public class ReviewController {
    private final ReviewService service;

    public ReviewController(ReviewService service) { this.service = service; }

    @GetMapping
    public List<ReviewResponse> listAll(@AuthenticationPrincipal String userId, @PathVariable UUID walkId) {
        return service.getAll(UUID.fromString(userId), walkId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@AuthenticationPrincipal String userId, @PathVariable UUID walkId,
                                  @Valid @RequestBody ReviewCreateRequest request) {
        return service.create(UUID.fromString(userId), walkId, request);
    }
}
