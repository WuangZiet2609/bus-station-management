package com.busstation.controller;

import com.busstation.dto.request.BookingCreateRequest;
import com.busstation.dto.request.BookingUpdateRequest;
import com.busstation.dto.response.BookingResponse;
import com.busstation.dto.response.PageResponse;
import com.busstation.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingCreateRequest request) {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(bookingService.createBooking(userId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<PageResponse<BookingResponse>> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(bookingService.getUserBookings(userId, page, size, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @GetMapping("/code/{bookingCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<BookingResponse> getBookingByCode(@PathVariable String bookingCode) {
        return ResponseEntity.ok(bookingService.getBookingByCode(bookingCode));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<BookingResponse> updateBooking(@PathVariable UUID id,
                                                         @Valid @RequestBody BookingUpdateRequest request) {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(bookingService.updateBooking(id, request, userId));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'CUSTOMER')")
    public ResponseEntity<Void> cancelBooking(@PathVariable UUID id,
                                              @RequestParam String reason) {
        UUID userId = getCurrentUserId();
        bookingService.cancelBooking(id, userId, reason);
        return ResponseEntity.ok().build();
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.busstation.security.UserPrincipal principal) {
            return principal.getId();
        }
        throw new RuntimeException("User not authenticated");
    }
}