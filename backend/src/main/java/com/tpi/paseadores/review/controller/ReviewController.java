package com.tpi.paseadores.review.controller;

import com.tpi.paseadores.review.dto.ReviewCreateRequest;
import com.tpi.paseadores.review.dto.ReviewResponse;
import com.tpi.paseadores.review.service.ReviewService;
import com.tpi.paseadores.review.dto.WalkerReviewResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ReviewController {
    private final ReviewService service;

    public ReviewController(ReviewService service) { this.service = service; }

    @GetMapping("/walks/{walkId}/reviews")
    public List<ReviewResponse> listAll(@AuthenticationPrincipal String userId, @PathVariable UUID walkId) {
        return service.getAll(UUID.fromString(userId), walkId);
    }

    @GetMapping("/walkers/{walkerId}/reviews")
    public List<WalkerReviewResponse> forWalker(@PathVariable UUID walkerId,
                                                @RequestParam(defaultValue = "0") int page) {
        return service.forWalker(walkerId, page);
    }

    @PostMapping("/walks/{walkId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@AuthenticationPrincipal String userId, @PathVariable UUID walkId, @Valid @RequestBody ReviewCreateRequest request) {
        return service.create(UUID.fromString(userId), walkId, request);
    }
}
