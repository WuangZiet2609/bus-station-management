package com.busstation.repository;

import com.busstation.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    List<Ticket> findByTripId(UUID tripId);
    List<Ticket> findByUserId(UUID userId);
}
