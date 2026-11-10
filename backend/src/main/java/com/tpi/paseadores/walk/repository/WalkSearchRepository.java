package com.tpi.paseadores.walk.repository;

import com.tpi.paseadores.pet.domain.PetSize;
import com.tpi.paseadores.walk.domain.BookingStatus;
import com.tpi.paseadores.walk.domain.WalkStatus;
import com.tpi.paseadores.walk.dto.ClientBookingResponse;
import com.tpi.paseadores.walk.dto.JoinedPetResponse;
import com.tpi.paseadores.walk.dto.WalkAgendaPeriod;
import com.tpi.paseadores.walk.dto.WalkReadResponse;
import com.tpi.paseadores.walk.dto.WalkSearchFilters;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// read model de walks: search (listado público), mine (agenda del paseador) y getById
// (detalle), los tres con el JOIN a walk_pets ya resuelto para los lugares libres, en vez
// de cargar entidades JPA una por una (N+1). Por eso no usa WalkRepository.
@Repository
public class WalkSearchRepository {

    private static final String DETAIL_SELECT = """
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
    private static final RowMapper<WalkReadResponse> DETAIL_ROW_MAPPER = (rs, row) -> new WalkReadResponse(
            rs.getObject("id", UUID.class), rs.getObject("zone_id", UUID.class), rs.getString("zone_name"),
            rs.getTimestamp("start_time").toLocalDateTime(), rs.getTimestamp("end_time").toLocalDateTime(),
            rs.getInt("duration_minutes"), rs.getString("meeting_point"), rs.getBigDecimal("price_per_pet"),
            rs.getInt("capacity"), rs.getInt("booked_spots"), rs.getInt("available_spots"),
            WalkStatus.valueOf(rs.getString("status")), new WalkReadResponse.WalkerSummary(
            rs.getObject("walker_id", UUID.class), rs.getString("full_name"), rs.getBigDecimal("rating_avg"),
            rs.getInt("review_count")));

    private final NamedParameterJdbcTemplate jdbc;

    public WalkSearchRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // mismo SELECT que findVisibleDetail/findAgenda (DETAIL_SELECT): la búsqueda pública es
    // el mismo read model de walk, solo con otro WHERE
    public List<WalkReadResponse> findAvailable(WalkSearchFilters filters, LocalDateTime now) {
        StringBuilder sql = new StringBuilder(DETAIL_SELECT + """
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
        return jdbc.query(sql.toString(), params, DETAIL_ROW_MAPPER);
    }

    public Optional<WalkReadResponse> findVisibleDetail(UUID walkId) {
        return jdbc.query(DETAIL_SELECT + " WHERE w.id = :id AND u.is_active = true AND u.role = 'WALKER'",
                new MapSqlParameterSource("id", walkId), DETAIL_ROW_MAPPER).stream().findFirst();
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
        return jdbc.query(DETAIL_SELECT + " WHERE w.walker_id = :walkerId" + condition
                + " ORDER BY w.start_time" + order + ", w.id LIMIT :limit OFFSET :offset", params, DETAIL_ROW_MAPPER);
    }

    private static final RowMapper<ClientBookingResponse> CLIENT_BOOKING_ROW_MAPPER = (rs, row) -> new ClientBookingResponse(
            rs.getObject("booking_id", UUID.class), BookingStatus.valueOf(rs.getString("booking_status")),
            rs.getObject("pet_id", UUID.class), rs.getString("pet_name"),
            rs.getObject("walk_id", UUID.class), WalkStatus.valueOf(rs.getString("walk_status")),
            rs.getTimestamp("start_time").toLocalDateTime(), rs.getTimestamp("end_time").toLocalDateTime(),
            rs.getString("meeting_point"), rs.getBigDecimal("price_per_pet"), rs.getString("zone_name"),
            new ClientBookingResponse.WalkerSummary(rs.getObject("walker_id", UUID.class), rs.getString("full_name"),
                    rs.getBigDecimal("rating_avg"), rs.getInt("review_count")));

    // read model de "mis reservas": simétrico a findAgenda pero partiendo de walk_pets del cliente,
    // no de walks del paseador
    public List<ClientBookingResponse> findBookingsForClient(UUID clientId, WalkAgendaPeriod period,
                                                               LocalDateTime now, int page, int size) {
        boolean upcoming = period == WalkAgendaPeriod.UPCOMING;
        String condition = upcoming
                ? " AND b.status = 'JOINED' AND w.end_time > :now"
                : " AND (b.status <> 'JOINED' OR w.end_time <= :now)";
        String order = upcoming ? " ASC" : " DESC";
        var params = new MapSqlParameterSource("clientId", clientId)
                .addValue("now", Timestamp.valueOf(now)).addValue("limit", size + 1)
                .addValue("offset", (long) page * size);
        String sql = """
                SELECT b.id AS booking_id, b.status AS booking_status, b.pet_id, p.name AS pet_name,
                       w.id AS walk_id, w.status AS walk_status, w.start_time, w.end_time,
                       w.meeting_point, w.price_per_pet, z.name AS zone_name,
                       u.id AS walker_id, u.full_name, u.rating_avg, u.review_count
                FROM walk_pets b
                JOIN pets p ON p.id = b.pet_id
                JOIN walks w ON w.id = b.walk_id
                JOIN zones z ON z.id = w.zone_id
                JOIN users u ON u.id = w.walker_id
                WHERE p.owner_id = :clientId
                """ + condition + " ORDER BY w.start_time" + order + ", b.id LIMIT :limit OFFSET :offset";
        return jdbc.query(sql, params, CLIENT_BOOKING_ROW_MAPPER);
    }

    private static final RowMapper<JoinedPetResponse> JOINED_PET_ROW_MAPPER = (rs, row) -> new JoinedPetResponse(
            rs.getObject("pet_id", UUID.class), rs.getString("pet_name"), rs.getString("breed"),
            rs.getString("size") == null ? null : PetSize.valueOf(rs.getString("size")), rs.getString("notes"),
            new JoinedPetResponse.OwnerSummary(rs.getString("owner_name"), rs.getString("owner_phone")));

    // read model de "perros de esta salida": las mascotas JOINED de un walk puntual, con los
    // datos de contacto de cada dueño. Solo la usa WalkService para el paseador dueño del walk.
    public List<JoinedPetResponse> findJoinedPets(UUID walkId) {
        return jdbc.query("""
                SELECT p.id AS pet_id, p.name AS pet_name, p.breed, p.size, p.notes,
                       u.full_name AS owner_name, u.phone AS owner_phone
                FROM walk_pets b
                JOIN pets p ON p.id = b.pet_id
                JOIN users u ON u.id = p.owner_id
                WHERE b.walk_id = :walkId AND b.status = 'JOINED'
                ORDER BY p.name
                """, new MapSqlParameterSource("walkId", walkId), JOINED_PET_ROW_MAPPER);
    }

    public boolean hasActivePet(UUID walkId, UUID ownerId) {
        Boolean exists = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM walk_pets b JOIN pets p ON p.id = b.pet_id
                    WHERE b.walk_id = :walkId AND b.status = 'JOINED' AND p.owner_id = :ownerId
                )
                """, new MapSqlParameterSource("walkId", walkId).addValue("ownerId", ownerId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }
}
