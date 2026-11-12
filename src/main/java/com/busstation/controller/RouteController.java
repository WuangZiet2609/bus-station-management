package com.busstation.controller;

import com.busstation.dto.request.RouteCreateRequest;
import com.busstation.dto.request.RouteUpdateRequest;
import com.busstation.dto.response.PageResponse;
import com.busstation.dto.response.RouteResponse;
import com.busstation.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*;

import java.util.List
import java.util.UUID

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<RouteResponse> createRoute(@Valid @RequestBody RouteCreateRequest request) {
        return ResponseEntity.ok(routeService.createRoute(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<RouteResponse>> getAllRoutes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(routeService.getAllRoutes(page, size, keyword));
    }

    @GetMapping("/search")
    public ResponseEntity<List<RouteResponse>> searchRoutes(
            @RequestParam String departure,
            @RequestParam String arrival) {
        return ResponseEntity.ok(routeService.searchRoutes(departure, arrival));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteResponse> getRouteById(@PathVariable UUID id) {
        return ResponseEntity.ok(routeService.getRouteById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<RouteResponse> updateRoute(@PathVariable UUID id,
                                                     @Valid @RequestBody RouteUpdateRequest request) {
        return ResponseEntity.ok(routeService.updateRoute(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRoute(@PathVariable UUID id) {
        routeService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<RouteResponse> changeStatus(@PathVariable UUID id,
                                                      @RequestParam Boolean isActive) {
        return ResponseEntity.ok(routeService.changeStatus(id, isActive));
    }
}