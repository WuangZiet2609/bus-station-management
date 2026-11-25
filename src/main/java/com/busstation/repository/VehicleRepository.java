package com.busstation.repository;

import com.busstation.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    boolean existsByLicensePlate(String licensePlate);
    boolean existsByVehicleCode(String vehicleCode);
    Optional<Vehicle> findByVehicleCode(String vehicleCode);
}
