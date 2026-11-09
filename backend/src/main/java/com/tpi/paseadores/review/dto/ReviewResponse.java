package com.tpi.paseadores.review.dto;

import com.tpi.paseadores.review.domain.Review;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponse(UUID id, UUID walkId, UUID authorId, int rating, String comment, LocalDateTime createdAt) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(review.getId(), review.getWalkId(), review.getAuthorId(), review.getRating(), review.getComment(), review.getCreatedAt());
    }
}
