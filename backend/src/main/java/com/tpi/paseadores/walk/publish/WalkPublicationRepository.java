package com.tpi.paseadores.walk.publish;

import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Repository
public class WalkPublicationRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public WalkPublicationRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Serializa las publicaciones del mismo paseador hasta el fin de la transacción,
    // incluso cuando todavía no tiene paseos para bloquear.
    public void lockSchedule(UUID walkerId) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(CAST(:key AS text), 0))",
                Map.of("key", "walk-publication:" + walkerId), (ResultSetExtractor<Void>) rows -> {
                    rows.next();
                    return null;
                });
    }

    public boolean hasOverlap(UUID walkerId, LocalDateTime start, LocalDateTime end) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM walks
                    WHERE walker_id = :walkerId
                      AND status IN ('PUBLISHED', 'FULL', 'IN_PROGRESS')
                      AND start_time < :endTime AND end_time > :startTime
                )
                """, Map.of("walkerId", walkerId, "startTime", Timestamp.valueOf(start),
                "endTime", Timestamp.valueOf(end)), Boolean.class));
    }
}
