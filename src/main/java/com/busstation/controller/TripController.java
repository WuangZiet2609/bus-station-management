package com.busstation.controller;

import com.busstation.dto.request.TripCreateRequest;
import com.busstation.dto.request.TripUpdateRequest;
import com.busstation.dto.response.TripResponse;
import com.busstation.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {
    private final TripService tripService;

    @GetMapping
    public List<TripResponse> findAll() {
        return tripService.findAll();
    }

    @GetMapping("/{id}")
    public TripResponse findById(@PathVariable UUID id) {
        return tripService.findById(id);
    }

    @GetMapping("/search")
    public List<TripResponse> search(@RequestParam(required = false) String from,
                                     @RequestParam(required = false) String to,
                                     @RequestParam(required = false) String date) {
        return tripService.search(from, to, date);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TripResponse create(@Valid @RequestBody TripCreateRequest request) {
        return tripService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TripResponse update(@PathVariable UUID id, @RequestBody TripUpdateRequest request) {
        return tripService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        tripService.delete(id);
    }
}
