package com.busstation.service;

import com.busstation.dto.request.VehicleCreateRequest;
import com.busstation.dto.request.VehicleUpdateRequest;
import com.busstation.entity.Vehicle;
import com.busstation.entity.VehicleStatus;
import com.busstation.exception.BusinessException;
import com.busstation.repository.VehicleRepository;
import com.busstation.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {
    private final VehicleRepository vehicleRepository;

    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }

    public Vehicle findById(UUID id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy xe"));
    }

    public Vehicle findByVehicleCode(String vehicleCode) {
        return vehicleRepository.findByVehicleCode(vehicleCode)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy xe"));
    }

    @Transactional
    public Vehicle create(VehicleCreateRequest request) {
        if (vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw BusinessException.conflict("Biển số xe đã tồn tại");
        }
        String vehicleCode = CodeGenerator.generate("VEH", vehicleRepository.count());
        return vehicleRepository.save(Vehicle.builder()
                .licensePlate(request.getLicensePlate())
                .vehicleCode(vehicleCode)
                .name(request.getName())
                .brand(request.getBrand())
                .model(request.getModel())
                .seatCount(request.getSeatCount())
                .status(VehicleStatus.ACTIVE)
                .build());
    }

    @Transactional
    public Vehicle update(UUID id, VehicleUpdateRequest request) {
        Vehicle vehicle = findById(id);
        if (request.getLicensePlate() != null && !request.getLicensePlate().equals(vehicle.getLicensePlate())
                && vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw BusinessException.conflict("Biển số xe đã tồn tại");
        }
        if (request.getLicensePlate() != null) vehicle.setLicensePlate(request.getLicensePlate());
        if (request.getName() != null) vehicle.setName(request.getName());
        if (request.getBrand() != null) vehicle.setBrand(request.getBrand());
        if (request.getModel() != null) vehicle.setModel(request.getModel());
        if (request.getSeatCount() != null) vehicle.setSeatCount(request.getSeatCount());
        if (request.getStatus() != null) {
            try {
                vehicle.setStatus(VehicleStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw BusinessException.badRequest("Trạng thái xe không hợp lệ");
            }
        }
        return vehicle;
    }

    public void delete(UUID id) {
        vehicleRepository.delete(findById(id));
    }
}
