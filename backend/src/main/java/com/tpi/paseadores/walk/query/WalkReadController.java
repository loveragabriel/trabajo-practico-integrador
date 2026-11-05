package com.tpi.paseadores.walk.query;

import com.tpi.paseadores.walk.query.dto.WalkAgendaPeriod;
import com.tpi.paseadores.walk.query.dto.WalkAgendaResponse;
import com.tpi.paseadores.walk.query.dto.WalkReadResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/walks")
public class WalkReadController {
    private final WalkReadService service;

    public WalkReadController(WalkReadService service) {
        this.service = service;
    }

    @GetMapping("/mine")
    public WalkAgendaResponse agenda(@AuthenticationPrincipal String userId,
                                     @RequestParam(defaultValue = "UPCOMING") WalkAgendaPeriod period,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "12") int size) {
        return service.agenda(UUID.fromString(userId), period, page, size);
    }

    @GetMapping("/{id}")
    public WalkReadResponse detail(@AuthenticationPrincipal String userId, @PathVariable UUID id) {
        return service.detail(UUID.fromString(userId), id);
    }
}
