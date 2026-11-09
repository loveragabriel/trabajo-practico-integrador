package com.tpi.paseadores.walk.repository;

import com.tpi.paseadores.walk.domain.Walk;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface WalkRepository extends JpaRepository<Walk, UUID> {

    // RN-40: SELECT ... FOR UPDATE on the walk row. Every write that depends on the spot count
    // (join, cancel, capacity change) loads the walk through here, so two clients can't both take
    // the last spot. The lock lasts until the surrounding transaction ends.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Walk w WHERE w.id = :id")
    Optional<Walk> findByIdForUpdate(@Param("id") UUID id);

    // RN-18: serializa las creaciones del mismo paseador hasta el fin de la transacción,
    // incluso cuando todavía no tiene paseos para bloquear.
    @Query(value = "SELECT pg_advisory_xact_lock(hashtextextended(CAST(:key AS text), 0))", nativeQuery = true)
    void lockWalkerSchedule(@Param("key") String key);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM walks
                WHERE walker_id = :walkerId
                  AND status IN ('PUBLISHED', 'FULL', 'IN_PROGRESS')
                  AND start_time < :endTime AND end_time > :startTime
            )
            """, nativeQuery = true)
    boolean hasOverlap(@Param("walkerId") UUID walkerId, @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);
}
