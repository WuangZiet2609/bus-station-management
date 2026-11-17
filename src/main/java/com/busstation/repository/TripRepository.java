package com.busstation.repository;

import com.busstation.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {
    boolean existsByTripCode(String tripCode);
    List<Trip> findAllByOrderByDepartureTimeAsc();
}
