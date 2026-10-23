package com.tpi.paseadores.walk;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class WalkSearchRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public WalkSearchRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<WalkSearchResponse> findAvailable(WalkSearchFilters filters, LocalDateTime now) {
        StringBuilder sql = new StringBuilder("""
                SELECT w.id, w.zone_id, z.name AS zone_name, w.start_time, w.duration_minutes,
                       w.meeting_point, w.price_per_pet, w.capacity,
                       w.capacity - COALESCE(b.joined_count, 0) AS available_spots,
                       u.id AS walker_id, u.full_name, u.rating_avg, u.review_count
                FROM walks w
                JOIN zones z ON z.id = w.zone_id
                JOIN users u ON u.id = w.walker_id
                LEFT JOIN (
                    SELECT walk_id, COUNT(*) AS joined_count
                    FROM walk_pets WHERE status = 'JOINED' GROUP BY walk_id
                ) b ON b.walk_id = w.id
                WHERE w.status = 'PUBLISHED' AND w.start_time > :now
                  AND u.is_active = true AND u.role = 'WALKER'
                  AND w.capacity > COALESCE(b.joined_count, 0)
                """);
        var params = new MapSqlParameterSource("now", Timestamp.valueOf(now))
                .addValue("limit", filters.size() + 1)
                .addValue("offset", (long) filters.page() * filters.size());
        if (filters.zoneId() != null) {
            sql.append(" AND w.zone_id = :zoneId");
            params.addValue("zoneId", filters.zoneId());
        }
        if (filters.startsFrom() != null) {
            sql.append(" AND w.start_time >= :startsFrom");
            params.addValue("startsFrom", Timestamp.valueOf(filters.startsFrom()));
        }
        if (filters.startsTo() != null) {
            sql.append(" AND w.start_time < :startsTo");
            params.addValue("startsTo", Timestamp.valueOf(filters.startsTo()));
        }
        sql.append(" ORDER BY w.start_time, w.id LIMIT :limit OFFSET :offset");
        return jdbc.query(sql.toString(), params, (rs, row) -> new WalkSearchResponse(
                rs.getObject("id", UUID.class), rs.getObject("zone_id", UUID.class), rs.getString("zone_name"),
                rs.getTimestamp("start_time").toLocalDateTime(), rs.getInt("duration_minutes"),
                rs.getString("meeting_point"), rs.getBigDecimal("price_per_pet"),
                rs.getInt("capacity"), rs.getInt("available_spots"),
                new WalkSearchResponse.WalkerSummary(rs.getObject("walker_id", UUID.class),
                        rs.getString("full_name"), rs.getBigDecimal("rating_avg"), rs.getInt("review_count"))));
    }
}
