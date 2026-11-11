package com.tpi.paseadores.walk.controller;

import com.tpi.paseadores.shared.dto.PageResponse;
import com.tpi.paseadores.shared.validation.Pagination;
import com.tpi.paseadores.walk.dto.BookingResponse;
import com.tpi.paseadores.walk.dto.ClientBookingPeriod;
import com.tpi.paseadores.walk.dto.ClientBookingResponse;
import com.tpi.paseadores.walk.dto.JoinedPetResponse;
import com.tpi.paseadores.walk.dto.WalkAgendaPeriod;
import com.tpi.paseadores.walk.dto.WalkCreateRequest;
import com.tpi.paseadores.walk.dto.WalkReadResponse;
import com.tpi.paseadores.walk.dto.WalkSearchFilters;
import com.tpi.paseadores.walk.dto.WalkStatusUpdateRequest;
import com.tpi.paseadores.walk.service.BookingService;
import com.tpi.paseadores.walk.service.WalkService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/walks")
public class WalkController {
    private final WalkService service;
    private final BookingService bookings;

    public WalkController(WalkService service, BookingService bookings) {
        this.service = service;
        this.bookings = bookings;
    }

    @PostMapping
    public ResponseEntity<WalkReadResponse> create(@AuthenticationPrincipal String userId,
                                               @Valid @RequestBody WalkCreateRequest request) {
        var created = service.create(UUID.fromString(userId), request);
        return ResponseEntity.created(URI.create("/api/walks/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<WalkReadResponse> search(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startsFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startsTo,
            @RequestParam(defaultValue = Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = Pagination.DEFAULT_SIZE) int size) {
        return service.search(UUID.fromString(userId), new WalkSearchFilters(zoneId, startsFrom, startsTo, page, size));
    }

    @GetMapping("/mine")
    public PageResponse<WalkReadResponse> mine(@AuthenticationPrincipal String userId,
                                   @RequestParam(defaultValue = "UPCOMING") WalkAgendaPeriod period,
                                   @RequestParam(defaultValue = Pagination.DEFAULT_PAGE) int page,
                                   @RequestParam(defaultValue = Pagination.DEFAULT_SIZE) int size) {
        return service.mine(UUID.fromString(userId), period, page, size);
    }

    @GetMapping("/{id}")
    public WalkReadResponse getById(@AuthenticationPrincipal String userId, @PathVariable UUID id) {
        return service.getById(UUID.fromString(userId), id);
    }

    @GetMapping("/{id}/pets")
    public List<JoinedPetResponse> joinedPets(@AuthenticationPrincipal String userId, @PathVariable UUID id) {
        return service.getJoinedPets(UUID.fromString(userId), id);
    }

    @PostMapping("/{id}/join/{petId}")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse join(@AuthenticationPrincipal String userId, @PathVariable UUID id,
                                 @PathVariable UUID petId) {
        return bookings.join(UUID.fromString(userId), id, petId);
    }

    @DeleteMapping("/{id}/join/{petId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelBooking(@AuthenticationPrincipal String userId, @PathVariable UUID id,
                               @PathVariable UUID petId) {
        bookings.cancelBooking(UUID.fromString(userId), id, petId);
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(@AuthenticationPrincipal String userId, @PathVariable UUID id,
                              @Valid @RequestBody WalkStatusUpdateRequest request) {
        service.updateStatus(UUID.fromString(userId), id, request.status());
    }

    @GetMapping("/bookings/mine")
    public PageResponse<ClientBookingResponse> myBookings(@AuthenticationPrincipal String userId,
                                              @RequestParam(defaultValue = "UPCOMING") ClientBookingPeriod period,
                                              @RequestParam(defaultValue = Pagination.DEFAULT_PAGE) int page,
                                              @RequestParam(defaultValue = Pagination.DEFAULT_SIZE) int size) {
        return bookings.myBookings(UUID.fromString(userId), period, page, size);
    }
}
