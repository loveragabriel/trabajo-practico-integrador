package com.tpi.paseadores.walk;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WalkRepository extends JpaRepository<Walk, UUID> {

    // RN-40: SELECT ... FOR UPDATE on the walk row. Every write that depends on the spot count
    // (join, cancel, capacity change) loads the walk through here, so two clients can't both take
    // the last spot. The lock lasts until the surrounding transaction ends.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Walk w WHERE w.id = :id")
    Optional<Walk> findByIdForUpdate(@Param("id") UUID id);
}
