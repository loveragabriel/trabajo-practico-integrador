package com.tpi.paseadores.review.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record WalkerReviewResponse(UUID id, int rating, String comment, LocalDateTime createdAt) {}
