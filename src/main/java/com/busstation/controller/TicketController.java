package com.busstation.controller;

import com.busstation.dto.request.TicketCreateRequest;
import com.busstation.dto.response.TicketResponse;
import com.busstation.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public TicketResponse book(@Valid @RequestBody TicketCreateRequest request) {
        return ticketService.book(request);
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("isAuthenticated()")
    public TicketResponse pay(@PathVariable UUID id) {
        return ticketService.pay(id);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public List<TicketResponse> myTickets() {
        return ticketService.myTickets();
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public List<TicketResponse> search(@RequestParam(required = false) UUID tripId,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) String paymentStatus) {
        return ticketService.search(tripId, status, paymentStatus);
    }
}
