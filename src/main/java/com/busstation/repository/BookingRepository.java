package com.busstation.repository;

import com.busstation.entity.Booking;
import com.busstation.entity.BookingStatus;
import com.busstation.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByBookingCode(String bookingCode);

    List<Booking> findByUserId(UUID userId);

    Page<Booking> findByUserId(UUID userId, Pageable pageable);

    List<Booking> findByTripId(UUID tripId);

    Page<Booking> findByTripId(UUID tripId, Pageable pageable);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByPaymentStatus(PaymentStatus paymentStatus);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status IN :statuses ORDER BY b.createdAt DESC")
    List<Booking> findByUserIdAndStatuses(@Param("userId") UUID userId, @Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b WHERE b.trip.id = :tripId AND b.status IN :statuses")
    List<Booking> findByTripIdAndStatuses(@Param("tripId") UUID tripId, @Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT b FROM Booking b WHERE b.expiresAt < :now AND b.status = 'PENDING' AND b.paymentStatus = 'PENDING'")
    List<Booking> findExpiredPendingBookings(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.trip.id = :tripId AND b.status IN :statuses")
    long countByTripIdAndStatuses(@Param("tripId") UUID tripId, @Param("statuses") List<BookingStatus> statuses);

    boolean existsByBookingCode(String bookingCode);
}