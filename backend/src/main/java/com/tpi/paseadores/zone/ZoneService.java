package com.tpi.paseadores.zone;

import com.tpi.paseadores.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
