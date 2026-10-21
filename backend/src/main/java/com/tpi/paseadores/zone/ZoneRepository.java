package com.tpi.paseadores.zone;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ZoneRepository extends Repository<Zone, UUID> {
    List<Zone> findAllByOrderByNameAsc();

    Optional<Zone> findById(UUID id);

    boolean existsById(UUID id);
}
