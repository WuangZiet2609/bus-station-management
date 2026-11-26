package com.busstation.repository;

import com.busstation.entity.Trip;
import com.busstation.entity.TripStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id = :id")
    Optional<Trip> findLockedById(UUID id);

    @Query("""
            select count(t) > 0 from Trip t
            where (:tripId is null or t.id <> :tripId)
              and t.status not in :closedStatuses
              and t.departureTime < :arrival and t.arrivalTime > :departure
              and (t.vehicle.id = :vehicleId or t.driver.id in :driverIds or t.coDriver.id in :driverIds)
            """)
    boolean hasScheduleConflict(UUID tripId, UUID vehicleId, Collection<UUID> driverIds,
                                LocalDateTime departure, LocalDateTime arrival,
                                Collection<TripStatus> closedStatuses);

    boolean existsByTripCode(String tripCode);
    Optional<Trip> findByTripCode(String tripCode);
    List<Trip> findAllByOrderByDepartureTimeAsc();
}
