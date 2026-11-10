package com.tpi.paseadores.review.service;

import com.tpi.paseadores.pet.dto.PetResponse;
import com.tpi.paseadores.pet.service.PetService;
import com.tpi.paseadores.review.dto.ReceivedReviewResponse;
import com.tpi.paseadores.review.dto.ReviewCreateRequest;
import com.tpi.paseadores.review.dto.ReviewResponse;
import com.tpi.paseadores.review.repository.ReviewReadRepository;
import com.tpi.paseadores.review.repository.ReviewRepository;
import com.tpi.paseadores.shared.exception.BusinessRuleViolationException;
import com.tpi.paseadores.shared.exception.ForbiddenActionException;
import com.tpi.paseadores.user.domain.User;
import com.tpi.paseadores.user.domain.UserRole;
import com.tpi.paseadores.user.service.UserService;
import com.tpi.paseadores.walk.domain.Walk;
import com.tpi.paseadores.walk.domain.WalkStatus;
import com.tpi.paseadores.walk.service.WalkPetService;
import com.tpi.paseadores.walk.service.WalkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    @Mock ReviewRepository reviews;
    @Mock ReviewReadRepository reviewReads;
    @Mock WalkService walkService;
    @Mock WalkPetService walkPets;
    @Mock UserService users;
    @Mock PetService pets;

    private final UUID authorId = UUID.randomUUID();
    private final UUID walkerId = UUID.randomUUID();
    private final UUID walkId = UUID.randomUUID();
    private final UUID petId = UUID.randomUUID();
    private ReviewService service;

    @BeforeEach
    void setUp() {
        service = new ReviewService(reviews, reviewReads, walkService, walkPets, users, pets);
    }

    @Test
    void aParticipantCanReviewACompletedWalkAndThatUpdatesTheWalkerRating() {
        actor(UserRole.CLIENT, true);
        Walk walk = walk(WalkStatus.COMPLETED);
        when(walkService.getWalkForReview(walkId)).thenReturn(walk);
        when(pets.getAll(authorId)).thenReturn(List.of(pet()));
        when(walkPets.hasJoinedBooking(walkId, List.of(petId))).thenReturn(true);
        when(reviews.existsByWalkIdAndAuthorId(walkId, authorId)).thenReturn(false);
        when(reviews.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = service.create(authorId, walkId, new ReviewCreateRequest(5, "Excelente"));

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.walkId()).isEqualTo(walkId);
        assertThat(response.authorId()).isEqualTo(authorId);
        verify(users).applyReviewRating(walkerId, 5);

        ArgumentCaptor<com.tpi.paseadores.review.domain.Review> captor =
                ArgumentCaptor.forClass(com.tpi.paseadores.review.domain.Review.class);
        verify(reviews).save(captor.capture());
        assertThat(captor.getValue().getComment()).isEqualTo("Excelente");
    }

    @Test
    void onlyActiveClientsCanReview() {
        actor(UserRole.WALKER, true);
        assertThatThrownBy(() -> service.create(authorId, walkId, new ReviewCreateRequest(5, null)))
                .isInstanceOf(ForbiddenActionException.class);
        verifyNoInteractions(walkService, walkPets, reviews);
    }

    @Test
    void rejectsReviewingAWalkThatIsNotCompleted() {
        actor(UserRole.CLIENT, true);
        when(walkService.getWalkForReview(walkId)).thenReturn(walk(WalkStatus.IN_PROGRESS));

        assertThatThrownBy(() -> service.create(authorId, walkId, new ReviewCreateRequest(5, null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Solo se puede calificar un paseo finalizado.");
        verifyNoInteractions(pets, reviews);
        verify(users, never()).applyReviewRating(any(), anyInt());
    }

    @Test
    void rejectsReviewingAWalkWithoutHavingParticipated() {
        actor(UserRole.CLIENT, true);
        when(walkService.getWalkForReview(walkId)).thenReturn(walk(WalkStatus.COMPLETED));
        when(pets.getAll(authorId)).thenReturn(List.of(pet()));
        when(walkPets.hasJoinedBooking(walkId, List.of(petId))).thenReturn(false);

        assertThatThrownBy(() -> service.create(authorId, walkId, new ReviewCreateRequest(5, null)))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessage("Solo quien participó del paseo puede calificarlo.");
        verifyNoInteractions(reviews);
        verify(users, never()).applyReviewRating(any(), anyInt());
    }

    @Test
    void rejectsReviewingTheSameWalkTwice() {
        actor(UserRole.CLIENT, true);
        when(walkService.getWalkForReview(walkId)).thenReturn(walk(WalkStatus.COMPLETED));
        when(pets.getAll(authorId)).thenReturn(List.of(pet()));
        when(walkPets.hasJoinedBooking(walkId, List.of(petId))).thenReturn(true);
        when(reviews.existsByWalkIdAndAuthorId(walkId, authorId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(authorId, walkId, new ReviewCreateRequest(5, null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Ya calificaste este paseo.");
        verify(reviews, never()).save(any());
        verify(users, never()).applyReviewRating(any(), anyInt());
    }

    // --- reseñas recibidas (getMine) -------------------------------------------------------------

    @Test
    void activeWalkerSeesTheirReceivedReviewsWithTheAuthorName() {
        User walker = new User();
        walker.setId(walkerId);
        walker.setRole(UserRole.WALKER);
        walker.setActive(true);
        when(users.findById(walkerId)).thenReturn(Optional.of(walker));
        var received = new ReceivedReviewResponse(UUID.randomUUID(), walkId, 5, "Excelente", LocalDateTime.now(), "Martina");
        when(reviewReads.findReceivedByWalker(walkerId)).thenReturn(List.of(received));

        assertThat(service.getMine(walkerId)).containsExactly(received);
    }

    @Test
    void clientsCannotReadReceivedReviews() {
        actor(UserRole.CLIENT, true);
        assertThatThrownBy(() -> service.getMine(authorId)).isInstanceOf(ForbiddenActionException.class);
        verifyNoInteractions(reviewReads);
    }

    @Test
    void inactiveOrMissingWalkersCannotReadReceivedReviews() {
        actor(UserRole.WALKER, false);
        assertThatThrownBy(() -> service.getMine(authorId)).isInstanceOf(ForbiddenActionException.class);
        when(users.findById(authorId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getMine(authorId)).isInstanceOf(ForbiddenActionException.class);
        verifyNoInteractions(reviewReads);
    }

    private void actor(UserRole role, boolean active) {
        User user = new User();
        user.setId(authorId);
        user.setRole(role);
        user.setActive(active);
        when(users.findById(authorId)).thenReturn(Optional.of(user));
    }

    private Walk walk(WalkStatus status) {
        Walk walk = new Walk(walkerId, UUID.randomUUID(), LocalDateTime.now().minusDays(1), 30, 4,
                BigDecimal.TEN, "Entrada de la plaza");
        ReflectionTestUtils.setField(walk, "id", walkId);
        ReflectionTestUtils.setField(walk, "status", status);
        return walk;
    }

    private PetResponse pet() {
        return new PetResponse(petId, "Luna", null, null, null, LocalDateTime.now());
    }
}
