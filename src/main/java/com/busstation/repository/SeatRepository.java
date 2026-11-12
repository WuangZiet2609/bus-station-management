package com.busstation.repository;

import com.busstation.entity.Seat;
import com.busstation.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID> {

    List<Seat> findByTripId(UUID tripId);

    List<Seat> findByTripIdAndStatus(UUID tripId, SeatStatus status);

    List<Seat> findByTripIdAndSeatNumberIn(UUID tripId, List<String> seatNumbers);

    @Query("SELECT s FROM Seat s WHERE s.trip.id = :tripId AND s.seatNumber IN :seatNumbers AND s.status = :status")
    List<Seat> findAvailableSeats(@Param("tripId") UUID tripId, @Param("seatNumbers") List<String> seatNumbers, @Param("status") SeatStatus status);

    long countByTripIdAndStatus(UUID tripId, SeatStatus status);

    @Query("UPDATE Seat s SET s.status = :newStatus WHERE s.trip.id = :tripId AND s.seatNumber IN :seatNumbers")
    int updateSeatStatus(@Param("tripId") UUID tripId, @Param("seatNumbers") List<String> seatNumbers, @Param("newStatus") SeatStatus newStatus);
}