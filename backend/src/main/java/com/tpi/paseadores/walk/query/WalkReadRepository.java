package com.tpi.paseadores.walk.query;

import com.tpi.paseadores.walk.WalkStatus;
import com.tpi.paseadores.walk.query.dto.WalkAgendaPeriod;
import com.tpi.paseadores.walk.query.dto.WalkReadResponse;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class WalkReadRepository {
    private static final String SELECT = """
            SELECT w.id, w.zone_id, z.name AS zone_name, w.start_time, w.end_time,
                   w.duration_minutes, w.meeting_point, w.price_per_pet, w.capacity, w.status,
                   COALESCE(b.joined_count, 0) AS booked_spots,
                   GREATEST(w.capacity - COALESCE(b.joined_count, 0), 0) AS available_spots,
                   u.id AS walker_id, u.full_name, u.rating_avg, u.review_count
            FROM walks w
            JOIN zones z ON z.id = w.zone_id
            JOIN users u ON u.id = w.walker_id
            LEFT JOIN (
                SELECT walk_id, COUNT(*) AS joined_count
                FROM walk_pets WHERE status = 'JOINED' GROUP BY walk_id
            ) b ON b.walk_id = w.id
            """;
    private static final RowMapper<WalkReadResponse> ROW_MAPPER = (rs, row) -> new WalkReadResponse(
            rs.getObject("id", UUID.class), rs.getObject("zone_id", UUID.class), rs.getString("zone_name"),
            rs.getTimestamp("start_time").toLocalDateTime(), rs.getTimestamp("end_time").toLocalDateTime(),
            rs.getInt("duration_minutes"), rs.getString("meeting_point"), rs.getBigDecimal("price_per_pet"),
            rs.getInt("capacity"), rs.getInt("booked_spots"), rs.getInt("available_spots"),
            WalkStatus.valueOf(rs.getString("status")), new WalkReadResponse.WalkerSummary(
            rs.getObject("walker_id", UUID.class), rs.getString("full_name"), rs.getBigDecimal("rating_avg"),
            rs.getInt("review_count")));

    private final NamedParameterJdbcTemplate jdbc;

    public WalkReadRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<WalkReadResponse> findVisibleDetail(UUID walkId) {
        return jdbc.query(SELECT + " WHERE w.id = :id AND u.is_active = true AND u.role = 'WALKER'",
                new MapSqlParameterSource("id", walkId), ROW_MAPPER).stream().findFirst();
    }

    public List<WalkReadResponse> findAgenda(UUID walkerId, WalkAgendaPeriod period,
                                            LocalDateTime now, int page, int size) {
        boolean upcoming = period == WalkAgendaPeriod.UPCOMING;
        String condition = upcoming
                ? " AND w.status IN ('PUBLISHED', 'FULL', 'IN_PROGRESS') AND w.end_time > :now"
                : " AND (w.status IN ('COMPLETED', 'CANCELLED') OR w.end_time <= :now)";
        String order = upcoming ? " ASC" : " DESC";
        var params = new MapSqlParameterSource("walkerId", walkerId)
                .addValue("now", Timestamp.valueOf(now)).addValue("limit", size + 1)
                .addValue("offset", (long) page * size);
        return jdbc.query(SELECT + " WHERE w.walker_id = :walkerId" + condition
                + " ORDER BY w.start_time" + order + ", w.id LIMIT :limit OFFSET :offset", params, ROW_MAPPER);
    }

    public boolean hasJoinedBooking(UUID walkId, UUID ownerId) {
        Boolean exists = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM walk_pets b JOIN pets p ON p.id = b.pet_id
                    WHERE b.walk_id = :walkId AND b.status = 'JOINED' AND p.owner_id = :ownerId
                )
                """, new MapSqlParameterSource("walkId", walkId).addValue("ownerId", ownerId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }
}
