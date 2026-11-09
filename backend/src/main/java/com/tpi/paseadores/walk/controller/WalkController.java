package com.tpi.paseadores.walk.controller;

import com.tpi.paseadores.walk.dto.BookingRequest;
import com.tpi.paseadores.walk.dto.BookingResponse;
import com.tpi.paseadores.walk.dto.WalkAgendaPeriod;
import com.tpi.paseadores.walk.dto.WalkAgendaResponse;
import com.tpi.paseadores.walk.dto.WalkCreateRequest;
import com.tpi.paseadores.walk.dto.WalkReadResponse;
import com.tpi.paseadores.walk.dto.WalkResponse;
import com.tpi.paseadores.walk.dto.WalkSearchFilters;
import com.tpi.paseadores.walk.dto.WalkSearchResponse;
import com.tpi.paseadores.walk.dto.WalkSearchPageResponse;
import com.tpi.paseadores.walk.service.WalkService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/walks")
public class WalkController {
    private final WalkService service;

    public WalkController(WalkService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<WalkResponse> create(@AuthenticationPrincipal String userId,
                                               @Valid @RequestBody WalkCreateRequest request) {
        var created = service.create(UUID.fromString(userId), request);
        return ResponseEntity.created(URI.create("/api/walks/" + created.id())).body(created);
    }

    @GetMapping
    public WalkSearchPageResponse search(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startsFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startsTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return service.search(UUID.fromString(userId), new WalkSearchFilters(zoneId, startsFrom, startsTo, page, size));
    }

    @GetMapping("/mine")
    public WalkAgendaResponse mine(@AuthenticationPrincipal String userId,
                                   @RequestParam(defaultValue = "UPCOMING") WalkAgendaPeriod period,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "12") int size) {
        return service.mine(UUID.fromString(userId), period, page, size);
    }

    @GetMapping("/by-walker/{walkerId}")
    public List<WalkSearchResponse> byWalker(@PathVariable UUID walkerId) {
        return service.byWalker(walkerId);
    }

    @GetMapping("/bookings/mine")
    public List<BookingResponse> bookings(@AuthenticationPrincipal String userId,
                                           @RequestParam(defaultValue = "0") int page) {
        return service.bookings(UUID.fromString(userId), null, page);
    }

    @GetMapping("/{id}/bookings")
    public List<BookingResponse> walkBookings(@AuthenticationPrincipal String userId, @PathVariable UUID id) {
        return service.bookings(UUID.fromString(userId), id, 0);
    }

    @PostMapping("/{id}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public void book(@AuthenticationPrincipal String userId, @PathVariable UUID id,
                     @Valid @RequestBody BookingRequest request) {
        service.book(UUID.fromString(userId), id, request.petIds());
    }

    @DeleteMapping("/{id}/bookings/{bookingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelBooking(@AuthenticationPrincipal String userId, @PathVariable UUID id,
                              @PathVariable UUID bookingId) {
        service.cancelBooking(UUID.fromString(userId), id, bookingId);
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal String userId, @PathVariable UUID id) {
        service.cancelWalk(UUID.fromString(userId), id);
    }

    @PostMapping("/{id}/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void start(@AuthenticationPrincipal String userId, @PathVariable UUID id) {
        service.startWalk(UUID.fromString(userId), id);
    }

    @PostMapping("/{id}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@AuthenticationPrincipal String userId, @PathVariable UUID id) {
        service.completeWalk(UUID.fromString(userId), id);
    }

    @GetMapping("/{id}")
    public WalkReadResponse getById(@AuthenticationPrincipal String userId, @PathVariable UUID id) {
        return service.getById(UUID.fromString(userId), id);
    }
}
