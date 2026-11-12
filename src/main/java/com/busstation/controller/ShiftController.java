package com.busstation.controller;

import com.busstation.dto.request.ShiftCreateRequest;
import com.busstation.dto.request.ShiftSearchRequest;
import com.busstation.dto.request.ShiftUpdateRequest;
import com.busstation.dto.response.PageResponse;
import com.busstation.dto.response.ShiftResponse;
import com.busstation.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ShiftResponse> createShift(@Valid @RequestBody ShiftCreateRequest request) {
        return ResponseEntity.ok(shiftService.createShift(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'DRIVER')")
    public ResponseEntity<PageResponse<ShiftResponse>> getShifts(
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ShiftSearchRequest request = ShiftSearchRequest.builder()
                .employeeId(employeeId)
                .dateFrom(dateFrom != null ? LocalDate.parse(dateFrom) : null)
                .dateTo(dateTo != null ? LocalDate.parse(dateTo) : null)
                .status(status)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(shiftService.getShifts(request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'DRIVER')")
    public ResponseEntity<PageResponse<ShiftResponse>> getMyShifts(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID employeeId = getCurrentUserId();
        ShiftSearchRequest request = ShiftSearchRequest.builder()
                .employeeId(employeeId)
                .dateFrom(dateFrom != null ? LocalDate.parse(dateFrom) : null)
                .dateTo(dateTo != null ? LocalDate.parse(dateTo) : null)
                .status(status)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(shiftService.getShifts(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShiftResponse> getShiftById(@PathVariable UUID id) {
        return ResponseEntity.ok(shiftService.getShiftById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ShiftResponse> updateShift(@PathVariable UUID id,
                                                     @Valid @RequestBody ShiftUpdateRequest request) {
        return ResponseEntity.ok(shiftService.updateShift(id, request));
    }

    @PatchMapping("/{id}/checkin")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'DRIVER')")
    public ResponseEntity<ShiftResponse> checkIn(@PathVariable UUID id) {
        return ResponseEntity.ok(shiftService.checkIn(id));
    }

    @PatchMapping("/{id}/checkout")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'DRIVER')")
    public ResponseEntity<ShiftResponse> checkOut(@PathVariable UUID id) {
        return ResponseEntity.ok(shiftService.checkOut(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteShift(@PathVariable UUID id) {
        shiftService.deleteShift(id);
        return ResponseEntity.noContent().build();
    }

    private UUID getCurrentUserId() {
        // In real implementation, get from SecurityContext
        return UUID.randomUUID();
    }
}