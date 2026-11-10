package com.tpi.paseadores.review.controller;

import com.tpi.paseadores.review.dto.ReceivedReviewResponse;
import com.tpi.paseadores.review.dto.ReviewCreateRequest;
import com.tpi.paseadores.review.dto.ReviewResponse;
import com.tpi.paseadores.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// cada método mapea su path completo (en vez de un @RequestMapping de clase): "mine" no
// cuelga de un walkId como los otros dos, así que no comparten el mismo prefijo
@RestController
public class ReviewController {
    private final ReviewService service;

    public ReviewController(ReviewService service) { this.service = service; }

    @GetMapping("/api/walks/{walkId}/reviews")
    public List<ReviewResponse> listAll(@AuthenticationPrincipal String userId, @PathVariable UUID walkId) {
        return service.getAll(UUID.fromString(userId), walkId);
    }

    @PostMapping("/api/walks/{walkId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@AuthenticationPrincipal String userId, @PathVariable UUID walkId,
                                  @Valid @RequestBody ReviewCreateRequest request) {
        return service.create(UUID.fromString(userId), walkId, request);
    }

    // reseñas que recibió el paseador autenticado, en todos sus paseos
    @GetMapping("/api/reviews")
    public List<ReceivedReviewResponse> mine(@AuthenticationPrincipal String userId) {
        return service.getMine(UUID.fromString(userId));
    }
}
