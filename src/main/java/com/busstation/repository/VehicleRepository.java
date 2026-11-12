package com.busstation.repository;

import com.busstation.entity.Vehicle;
import com.busstation.entity.VehicleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    List<Vehicle> findByStatus(VehicleStatus status);

    Page<Vehicle> findByStatus(VehicleStatus status, Pageable pageable);

    @Query("SELECT v FROM Vehicle v WHERE v.status = :status AND v.seatCount >= :minSeats")
    List<Vehicle> findAvailableVehicles(@Param("status") VehicleStatus status, @Param("minSeats") Integer minSeats);

    boolean existsByLicensePlate(String licensePlate);
}