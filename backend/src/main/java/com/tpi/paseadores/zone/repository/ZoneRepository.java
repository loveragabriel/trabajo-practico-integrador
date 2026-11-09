package com.tpi.paseadores.zone.repository;

import com.tpi.paseadores.zone.domain.Zone;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ZoneRepository extends Repository<Zone, UUID> {
    List<Zone> findAllByOrderByNameAsc();

    Optional<Zone> findById(UUID id);

    boolean existsById(UUID id);

    List<Zone> findAllByIdIn(Collection<UUID> ids);
}
