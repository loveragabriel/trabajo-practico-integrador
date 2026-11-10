package com.tpi.paseadores.review.repository;

import com.tpi.paseadores.review.dto.ReceivedReviewResponse;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

// read model de "reseñas que recibió este paseador": junta reviews -> walks -> users (autor).
// ReviewRepository (JPA) no alcanza para esto porque Review no mapea una relación a Walk.
@Repository
public class ReviewReadRepository {
    private static final RowMapper<ReceivedReviewResponse> ROW_MAPPER = (rs, row) -> new ReceivedReviewResponse(
            rs.getObject("id", UUID.class), rs.getObject("walk_id", UUID.class), rs.getInt("rating"),
            rs.getString("comment"), rs.getTimestamp("created_at").toLocalDateTime(), rs.getString("author_name"));

    private final NamedParameterJdbcTemplate jdbc;

    public ReviewReadRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ReceivedReviewResponse> findReceivedByWalker(UUID walkerId) {
        return jdbc.query("""
                SELECT r.id, r.walk_id, r.rating, r.comment, r.created_at, u.full_name AS author_name
                FROM reviews r
                JOIN walks w ON w.id = r.walk_id
                JOIN users u ON u.id = r.author_id
                WHERE w.walker_id = :walkerId
                ORDER BY r.created_at DESC
                """, new MapSqlParameterSource("walkerId", walkerId), ROW_MAPPER);
    }
}
