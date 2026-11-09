package com.tpi.paseadores.zone.service;

import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import com.tpi.paseadores.zone.dto.ZoneResponse;
import com.tpi.paseadores.zone.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ZoneService {
    private final ZoneRepository repository;

    public ZoneService(ZoneRepository repository) {
        this.repository = repository;
    }

    public List<ZoneResponse> findAll() {
        return repository.findAllByOrderByNameAsc().stream().map(ZoneResponse::from).toList();
    }

    public ZoneResponse findById(UUID id) {
        return repository.findById(id).map(ZoneResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found"));
    }

    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    // usado por el módulo de paseadores y cobertura para resolver sus walker_zones
    public List<ZoneResponse> findAllById(List<UUID> ids) {
        return repository.findAllByIdIn(ids).stream().map(ZoneResponse::from)
                .sorted(Comparator.comparing(ZoneResponse::name)).toList();
    }
}
