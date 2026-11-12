package com.busstation.controller;

import com.busstation.dto.response.PageResponse;
import com.busstation.dto.response.TripResponse;
import com.busstation.dto.response.RouteResponse;
import com.busstation.service.TripService;
import com.busstation.service.RouteService;
import lombok.RequiredArgsConstructor
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.List
import java.util.UUID

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final TripService tripService;
    private final RouteService routeService;

    @GetMapping("/trips/search")
    public ResponseEntity<PageResponse<TripResponse>> searchTrips(
            @RequestParam UUID routeId,
            @RequestParam String departureDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        LocalDate date = LocalDate.parse(departureDate);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        // Use TripService to search
        return ResponseEntity.ok(tripService.searchTrips(
                com.busstation.dto.request.TripSearchRequest.builder()
                        .routeId(routeId)
                        .departureDate(date)
                        .page(page)
                        .size(size)
                        .build()
        ));
    }

    @GetMapping("/routes")
    public ResponseEntity<List<RouteResponse>> getActiveRoutes() {
        return ResponseEntity.ok(routeService.getAllRoutes(0, 100, null).getContent());
    }

    @GetMapping("/routes/search")
    public ResponseEntity<List<RouteResponse>> searchRoutes(
            @RequestParam String departure,
            @RequestParam String arrival) {
        return ResponseEntity.ok(routeService.searchRoutes(departure, arrival));
    }

    @GetMapping("/trips/{id}/seats")
    public ResponseEntity<com.busstation.dto.response.TripSeatsResponse> getTripSeats(@PathVariable UUID id) {
        return ResponseEntity.ok(tripService.getTripSeats(id));
    }
}