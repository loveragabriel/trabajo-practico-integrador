package com.tpi.paseadores.zone;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/zones")
public class ZoneController {
    private final ZoneService service;

    public ZoneController(ZoneService service) {
        this.service = service;
    }

    @GetMapping
    public List<ZoneResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ZoneResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }
}
