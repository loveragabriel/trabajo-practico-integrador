package com.tpi.paseadores.review.repository;

import com.tpi.paseadores.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findAllByWalkId(UUID walkId);
}
