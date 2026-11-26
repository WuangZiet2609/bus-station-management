package com.busstation.repository;

import com.busstation.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    @Query("select distinct t.seatNumber from Ticket t where t.trip.id = :tripId order by t.seatNumber")
    List<Integer> findOccupiedSeats(UUID tripId);

    boolean existsByTripId(UUID tripId);
    boolean existsByTripIdAndSeatNumber(UUID tripId, Integer seatNumber);
    long countByTripId(UUID tripId);
    boolean existsByTripIdAndSeatNumberGreaterThan(UUID tripId, Integer seatNumber);
    boolean existsByTicketCode(String ticketCode);
    Optional<Ticket> findByTicketCode(String ticketCode);
    List<Ticket> findByTripId(UUID tripId);
    List<Ticket> findByUserId(UUID userId);
}
