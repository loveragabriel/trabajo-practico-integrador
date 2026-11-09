package com.tpi.paseadores.review.service;

import com.tpi.paseadores.pet.dto.PetResponse;
import com.tpi.paseadores.pet.service.PetService;
import com.tpi.paseadores.review.domain.Review;
import com.tpi.paseadores.review.dto.ReviewCreateRequest;
import com.tpi.paseadores.review.dto.ReviewResponse;
import com.tpi.paseadores.review.repository.ReviewRepository;
import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.user.domain.UserRole;
import com.tpi.paseadores.user.service.UserService;
import com.tpi.paseadores.walk.domain.Walk;
import com.tpi.paseadores.walk.domain.WalkStatus;
import com.tpi.paseadores.walk.service.WalkPetService;
import com.tpi.paseadores.walk.service.WalkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// reglas de reseñas (docs/modulos.md #9): un dueño que participó (reserva JOINED) califica un
// paseo COMPLETED, una sola vez por paseo, y eso actualiza el promedio del paseador.
// No alcanza con el propio repositorio: para validar esas reglas necesita datos de otras
// features (el paseo, si participó, su mascota), así que depende de los services de walk/
// pet/user en vez de sus repositorios, igual que hace BookingService con las reservas.
@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final WalkService walkService;
    private final WalkPetService walkPetService;
    private final UserService userService;
    private final PetService petService;

    @Transactional(readOnly = true)
    public List<ReviewResponse> getAll(UUID userId, UUID walkId) {
        return reviewRepository.findAllByWalkId(walkId).stream().map(ReviewResponse::from).toList();
    }

    @Transactional
    public ReviewResponse create(UUID userId, UUID walkId, ReviewCreateRequest request) {
        boolean isActiveClient = userService.findById(userId)
                .filter(u -> u.isActive() && u.getRole() == UserRole.CLIENT)
                .isPresent();
        if (!isActiveClient) {
            throw new ForbiddenActionException("Solo clientes activos pueden calificar un paseo");
        }

        Walk walk = walkService.getWalkForReview(walkId);
        if (walk.getStatus() != WalkStatus.COMPLETED) {
            throw new BusinessRuleViolationException("Solo se puede calificar un paseo finalizado.");
        }

        List<UUID> ownPetIds = petService.getAll(userId).stream().map(PetResponse::id).toList();
        if (!walkPetService.hasJoinedBooking(walkId, ownPetIds)) {
            throw new ForbiddenActionException("Solo quien participó del paseo puede calificarlo.");
        }
        if (reviewRepository.existsByWalkIdAndAuthorId(walkId, userId)) {
            throw new BusinessRuleViolationException("Ya calificaste este paseo.");
        }

        Review review = new Review();
        review.setAuthorId(userId);
        review.setWalkId(walkId);
        review.setRating(request.rating());
        review.setComment(request.comment() != null ? request.comment().trim() : null);
        Review saved = reviewRepository.save(review);

        userService.applyReviewRating(walk.getWalkerId(), request.rating());
        return ReviewResponse.from(saved);
    }
}
