package com.busstation.controller;

import com.busstation.dto.request.TripCreateRequest;
import com.busstation.dto.request.TripSearchRequest;
import com.busstation.dto.request.TripUpdateRequest;
import com.busstation.dto.response.PageResponse;
import com.busstation.dto.response.TripResponse;
import com.busstation.dto.response.TripSeatsResponse;
import com.busstation.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<TripResponse> createTrip(@Valid @RequestBody TripCreateRequest request) {
        return ResponseEntity.ok(tripService.createTrip(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<TripResponse>> searchTrips(
            @RequestParam(required = false) UUID routeId,
            @RequestParam(required = false) String departureDate,
            @RequestParam(required = false) String departureTimeFrom,
            @RequestParam(required = false) String departureTimeTo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "departureTime") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        TripSearchRequest request = TripSearchRequest.builder()
                .routeId(routeId)
                .departureDate(departureDate != null ? java.time.LocalDate.parse(departureDate) : null)
                .departureTimeFrom(departureTimeFrom != null ? java.time.LocalDateTime.parse(departureTimeFrom) : null)
                .departureTimeTo(departureTimeTo != null ? java.time.LocalDateTime.parse(departureTimeTo) : null)
                .status(status)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();

        return ResponseEntity.ok(tripService.searchTrips(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponse> getTripById(@PathVariable UUID id) {
        return ResponseEntity.ok(tripService.getTripById(id));
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<TripSeatsResponse> getTripSeats(@PathVariable UUID id) {
        return ResponseEntity.ok(tripService.getTripSeats(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<TripResponse> updateTrip(@PathVariable UUID id,
                                                   @Valid @RequestBody TripUpdateRequest request) {
        return ResponseEntity.ok(tripService.updateTrip(id, request));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Void> cancelTrip(@PathVariable UUID id,
                                           @RequestParam String reason) {
        UUID userId = getCurrentUserId();
        tripService.cancelTrip(id, userId, reason);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'DRIVER')")
    public ResponseEntity<Void> startTrip(@PathVariable UUID id) {
        tripService.startTrip(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'DRIVER')")
    public ResponseEntity<Void> completeTrip(@PathVariable UUID id) {
        tripService.completeTrip(id);
        return ResponseEntity.ok().build();
    }

    private UUID getCurrentUserId() {
        // In real implementation, get from SecurityContext
        return UUID.randomUUID();
    }
}