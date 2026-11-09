package com.tpi.paseadores.user.repository;

import com.tpi.paseadores.user.domain.WalkerZone;
import com.tpi.paseadores.user.domain.WalkerZoneId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WalkerZoneRepository extends JpaRepository<WalkerZone, WalkerZoneId> {
    List<WalkerZone> findAllByWalkerId(UUID walkerId);

    void deleteAllByWalkerId(UUID walkerId);
}
