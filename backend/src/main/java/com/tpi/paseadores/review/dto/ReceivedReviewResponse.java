package com.tpi.paseadores.review.dto;

import java.time.LocalDateTime;
import java.util.UUID;

// una reseña recibida por el paseador dueño del token, con el nombre de quien la escribió
public record ReceivedReviewResponse(UUID id, UUID walkId, int rating, String comment,
                                      LocalDateTime createdAt, String authorName) {}
