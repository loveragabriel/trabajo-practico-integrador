package com.tpi.paseadores.walk;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/walks")
public class WalkSearchController {
    private final WalkSearchService service;

    public WalkSearchController(WalkSearchService service) {
        this.service = service;
    }

    @GetMapping
    public WalkSearchPageResponse search(
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startsFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startsTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return service.search(new WalkSearchFilters(zoneId, startsFrom, startsTo, page, size));
    }
}
