package com.tpi.paseadores.review.repository;

import com.tpi.paseadores.review.domain.Review;
import com.tpi.paseadores.review.dto.WalkerReviewResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findAllByWalkIdOrderByCreatedAtDesc(UUID walkId);
    boolean existsByWalkIdAndAuthorId(UUID walkId, UUID authorId);

    @Query("""
            select new com.tpi.paseadores.review.dto.WalkerReviewResponse(r.id, r.rating, r.comment, r.createdAt)
            from Review r, Walk w where w.id = r.walkId and w.walkerId = :walkerId
            order by r.createdAt desc, r.id
            """)
    List<WalkerReviewResponse> findByWalker(UUID walkerId, Pageable pageable);

    @Query(value = """
            SELECT count(*) FROM reviews r JOIN walks w ON w.id = r.walk_id WHERE w.walker_id = :walkerId
            """, nativeQuery = true)
    int countByWalker(UUID walkerId);

    @Query(value = """
            SELECT round(avg(r.rating), 1) FROM reviews r JOIN walks w ON w.id = r.walk_id WHERE w.walker_id = :walkerId
            """, nativeQuery = true)
    java.math.BigDecimal averageByWalker(UUID walkerId);
}
