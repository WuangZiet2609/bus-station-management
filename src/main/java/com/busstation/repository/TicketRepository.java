package com.busstation.repository;

import com.busstation.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    boolean existsByTicketCode(String ticketCode);
    Optional<Ticket> findByTicketCode(String ticketCode);
    List<Ticket> findByTripId(UUID tripId);
    List<Ticket> findByUserId(UUID userId);
}
