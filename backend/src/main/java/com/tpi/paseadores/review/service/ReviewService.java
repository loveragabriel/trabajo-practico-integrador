package com.tpi.paseadores.review.service;

import com.tpi.paseadores.review.domain.Review;
import com.tpi.paseadores.review.dto.ReviewCreateRequest;
import com.tpi.paseadores.review.dto.ReviewResponse;
import com.tpi.paseadores.review.dto.WalkerReviewResponse;
import com.tpi.paseadores.review.repository.ReviewRepository;
import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.shared.exception.InvalidFieldException;
import com.tpi.paseadores.shared.validation.StringUtils;
import com.tpi.paseadores.user.service.UserService;
import com.tpi.paseadores.user.service.WalkerCoverageService;
import com.tpi.paseadores.walk.service.WalkService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserService userService;
    private final WalkService walks;
    private final WalkerCoverageService coverage;

    @Transactional(readOnly = true)
    public List<ReviewResponse> getAll(UUID userId, UUID walkId) {
        walks.getById(userId, walkId);
        return reviewRepository.findAllByWalkIdOrderByCreatedAtDesc(walkId).stream()
                .map(ReviewResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<WalkerReviewResponse> forWalker(UUID walkerId, int page) {
        if (page < 0) throw new InvalidFieldException("page", "La página no puede ser negativa.");
        coverage.getPublicProfile(walkerId);
        return reviewRepository.findByWalker(walkerId, PageRequest.of(page, 10));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReviewResponse create(UUID userId, UUID walkId, ReviewCreateRequest request) {
        if (request.walkId() != null && !walkId.equals(request.walkId())) {
            throw new InvalidFieldException("walkId", "La reseña debe corresponder al paseo indicado en la URL.");
        }
        UUID walkerId = walks.reviewableWalker(userId, walkId);
        var walker = userService.lockForUpdate(walkerId);
        if (reviewRepository.existsByWalkIdAndAuthorId(walkId, userId)) {
            throw new BusinessRuleViolationException("Ya calificaste este paseo.");
        }
        Review review = new Review();
        review.setAuthorId(userId);
        review.setWalkId(walkId);
        review.setRating(request.rating());
        review.setComment(StringUtils.blankToNull(request.comment()));
        var saved = reviewRepository.saveAndFlush(review);
        walker.setRatingAvg(reviewRepository.averageByWalker(walkerId));
        walker.setReviewCount(reviewRepository.countByWalker(walkerId));
        return ReviewResponse.from(saved);
    }
}
