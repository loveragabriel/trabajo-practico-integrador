package com.tpi.paseadores.review;

import com.tpi.paseadores.review.dto.ReviewRequest;
import com.tpi.paseadores.review.dto.ReviewResponse;

import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.user.User;
import com.tpi.paseadores.user.UserRole;
import com.tpi.paseadores.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<ReviewResponse> getAll(UUID userId, UUID walkId) {
        return reviewRepository.findAllByWalkId(walkId).stream().map(ReviewResponse::from).toList();
    }

    @Transactional()
    public ReviewResponse create(UUID userId, ReviewRequest request) {
        boolean isActiveClient = this.userService.findById(userId)
                .filter(u -> u.isActive() && u.getRole() == UserRole.CLIENT)
                .isPresent();
        if (!isActiveClient) {
            throw new ForbiddenActionException("Solo clientes activos pueden calificar un paseo");
        }

        Review review = new Review();
        review.setAuthorId(userId);
        review.setWalkId(request.walkId());
        review.setRating(request.rating());
        review.setComment(request.comment() != null ? request.comment().trim() : null);
        return ReviewResponse.from(reviewRepository.save(review));
    }
}
