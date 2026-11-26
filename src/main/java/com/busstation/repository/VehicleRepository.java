package com.busstation.repository;

import com.busstation.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Vehicle r where r.id = :id")
    Optional<Vehicle> findLockedById(UUID id);

    boolean existsByLicensePlate(String licensePlate);
    boolean existsByVehicleCode(String vehicleCode);
    Optional<Vehicle> findByVehicleCode(String vehicleCode);
}
