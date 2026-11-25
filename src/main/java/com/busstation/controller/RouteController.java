package com.busstation.controller;

import com.busstation.dto.request.RouteCreateRequest;
import com.busstation.dto.request.RouteUpdateRequest;
import com.busstation.entity.Route;
import com.busstation.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {
    private final RouteService routeService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public List<Route> findAll() {
        return routeService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public Route findById(@PathVariable UUID id) {
        return routeService.findById(id);
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("permitAll()")
    public Route findByCode(@PathVariable String code) {
        return routeService.findByCode(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Route create(@Valid @RequestBody RouteCreateRequest request) {
        return routeService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Route update(@PathVariable UUID id, @RequestBody RouteUpdateRequest request) {
        return routeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        routeService.delete(id);
    }
}
