package com.busstation.repository;

import com.busstation.entity.Trip;
import com.busstation.entity.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    Optional<Trip> findByTripCode(String tripCode);

    List<Trip> findByRouteIdAndDepartureTimeBetween(UUID routeId, LocalDateTime start, LocalDateTime end);

    List<Trip> findByDriverIdAndStatusIn(UUID driverId, List<TripStatus> statuses);

    List<Trip> findByVehicleIdAndDepartureTimeBetween(UUID vehicleId, LocalDateTime start, LocalDateTime end);

    Page<Trip> findByStatus(TripStatus status, Pageable pageable);

    @Query("SELECT t FROM Trip t WHERE t.route.id = :routeId AND DATE(t.departureTime) = :date AND t.status IN :statuses")
    List<Trip> findByRouteAndDate(@Param("routeId") UUID routeId, @Param("date") LocalDate date, @Param("statuses") List<TripStatus> statuses);

    @Query("SELECT t FROM Trip t WHERE t.departureTime BETWEEN :start AND :end AND t.status IN :statuses")
    List<Trip> findByDepartureTimeBetweenAndStatuses(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("statuses") List<TripStatus> statuses);

    @Query("SELECT t FROM Trip t WHERE t.status = :status AND t.departureTime <= :now")
    List<Trip> findOverdueTrips(@Param("status") TripStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT t FROM Trip t WHERE t.status = 'SCHEDULED' AND t.departureTime BETWEEN :now AND :future")
    List<Trip> findUpcomingTrips(@Param("now") LocalDateTime now, @Param("future") LocalDateTime future);

    boolean existsByTripCode(String tripCode);
}