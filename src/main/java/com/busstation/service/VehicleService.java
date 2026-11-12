package com.busstation.service;

import com.busstation.dto.request.VehicleCreateRequest;
import com.busstation.dto.request.VehicleUpdateRequest;
import com.busstation.dto.response.PageResponse;
import com.busstation.dto.response.VehicleResponse;
import com.busstation.entity.Vehicle;
import com.busstation.entity.VehicleStatus;
import com.busstation.exception.BusinessException;
import com.busstation.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    @Transactional
    public VehicleResponse createVehicle(VehicleCreateRequest request) {
        if (vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw BusinessException.conflict("Biển số xe đã tồn tại");
        }

        Vehicle vehicle = Vehicle.builder()
                .licensePlate(request.getLicensePlate())
                .name(request.getName())
                .brand(request.getBrand())
                .model(request.getModel())
                .yearOfManufacture(request.getYearOfManufacture())
                .seatCount(request.getSeatCount())
                .seatLayout(request.getSeatLayout())
                .vehicleType(request.getVehicleType())
                .fuelType(request.getFuelType())
                .status(VehicleStatus.ACTIVE)
                .registrationDate(request.getRegistrationDate())
                .insuranceExpiryDate(request.getInsuranceExpiryDate())
                .inspectionExpiryDate(request.getInspectionExpiryDate())
                .imageUrls(request.getImageUrls())
                .notes(request.getNotes())
                .build();

        vehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle created: {}", vehicle.getLicensePlate());

        return buildVehicleResponse(vehicle);
    }

    @Transactional(readOnly = true)
    public PageResponse<VehicleResponse> getAllVehicles(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Vehicle> vehiclePage;
        if (status != null) {
            vehiclePage = vehicleRepository.findByStatus(VehicleStatus.valueOf(status.toUpperCase()), pageable);
        } else {
            vehiclePage = vehicleRepository.findAll(pageable);
        }

        return PageResponse.<VehicleResponse>builder()
                .content(vehiclePage.map(this::buildVehicleResponse).getContent())
                .page(vehiclePage.getNumber())
                .size(vehiclePage.getSize())
                .totalElements(vehiclePage.getTotalElements())
                .totalPages(vehiclePage.getTotalPages())
                .first(vehiclePage.isFirst())
                .last(vehiclePage.isLast())
                .empty(vehiclePage.isEmpty())
                .build();
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(UUID id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy xe"));
        return buildVehicleResponse(vehicle);
    }

    @Transactional
    public VehicleResponse updateVehicle(UUID id, VehicleUpdateRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy xe"));

        if (request.getName() != null) vehicle.setName(request.getName());
        if (request.getBrand() != null) vehicle.setBrand(request.getBrand());
        if (request.getModel() != null) vehicle.setModel(request.getModel());
        if (request.getYearOfManufacture() != null) vehicle.setYearOfManufacture(request.getYearOfManufacture());
        if (request.getSeatCount() != null) vehicle.setSeatCount(request.getSeatCount());
        if (request.getSeatLayout() != null) vehicle.setSeatLayout(request.getSeatLayout());
        if (request.getVehicleType() != null) vehicle.setVehicleType(request.getVehicleType());
        if (request.getFuelType() != null) vehicle.setFuelType(request.getFuelType());
        if (request.getStatus() != null) vehicle.setStatus(VehicleStatus.valueOf(request.getStatus().toUpperCase()));
        if (request.getInsuranceExpiryDate() != null) vehicle.setInsuranceExpiryDate(request.getInsuranceExpiryDate());
        if (request.getInspectionExpiryDate() != null) vehicle.setInspectionExpiryDate(request.getInspectionExpiryDate());
        if (request.getImageUrls() != null) vehicle.setImageUrls(request.getImageUrls());
        if (request.getNotes() != null) vehicle.setNotes(request.getNotes());

        vehicle = vehicleRepository.save(vehicle);
        return buildVehicleResponse(vehicle);
    }

    @Transactional
    public void deleteVehicle(UUID id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy xe"));
        vehicleRepository.delete(vehicle);
    }

    @Transactional
    public VehicleResponse changeStatus(UUID id, String status) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy xe"));

        vehicle.setStatus(VehicleStatus.valueOf(status.toUpperCase()));
        vehicle = vehicleRepository.save(vehicle);

        return buildVehicleResponse(vehicle);
    }

    private VehicleResponse buildVehicleResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .licensePlate(vehicle.getLicensePlate())
                .name(vehicle.getName())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .yearOfManufacture(vehicle.getYearOfManufacture())
                .seatCount(vehicle.getSeatCount())
                .seatLayout(vehicle.getSeatLayout())
                .vehicleType(vehicle.getVehicleType())
                .fuelType(vehicle.getFuelType())
                .status(vehicle.getStatus().name())
                .registrationDate(vehicle.getRegistrationDate())
                .insuranceExpiryDate(vehicle.getInsuranceExpiryDate())
                .inspectionExpiryDate(vehicle.getInspectionExpiryDate())
                .imageUrls(vehicle.getImageUrls())
                .notes(vehicle.getNotes())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }
}