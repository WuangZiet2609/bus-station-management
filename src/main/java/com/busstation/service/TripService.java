package com.busstation.service;

import com.busstation.dto.request.TripCreateRequest;
import com.busstation.dto.request.TripUpdateRequest;
import com.busstation.dto.response.TripResponse;
import com.busstation.entity.*;
import com.busstation.exception.BusinessException;
import com.busstation.repository.RouteRepository;
import com.busstation.repository.TripRepository;
import com.busstation.repository.UserRepository;
import com.busstation.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TripService {
    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TripResponse> findAll() {
        return tripRepository.findAllByOrderByDepartureTimeAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TripResponse findById(UUID id) {
        return toResponse(tripRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy chuyến xe")));
    }

    @Transactional
    public TripResponse create(TripCreateRequest request) {
        if (tripRepository.existsByTripCode(request.getTripCode())) {
            throw BusinessException.conflict("Mã chuyến đã tồn tại");
        }
        validateTimes(request.getDepartureTime(), request.getArrivalTime());
        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy tuyến đường"));
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy xe"));
        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw BusinessException.badRequest("Xe không hoạt động");
        }

        Trip trip = Trip.builder()
                .tripCode(request.getTripCode())
                .route(route)
                .vehicle(vehicle)
                .driver(findDriver(request.getDriverId()))
                .coDriver(findDriver(request.getCoDriverId()))
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .basePrice(request.getBasePrice())
                .status(TripStatus.SCHEDULED)
                .availableSeats(vehicle.getSeatCount())
                .totalSeats(vehicle.getSeatCount())
                .notes(request.getNotes())
                .build();
        return toResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse update(UUID id, TripUpdateRequest request) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy chuyến xe"));
        if (request.getTripCode() != null && !request.getTripCode().equals(trip.getTripCode())
                && tripRepository.existsByTripCode(request.getTripCode())) {
            throw BusinessException.conflict("Mã chuyến đã tồn tại");
        }
        if (request.getTripCode() != null) trip.setTripCode(request.getTripCode());
        if (request.getRouteId() != null) {
            trip.setRoute(routeRepository.findById(request.getRouteId())
                    .orElseThrow(() -> BusinessException.notFound("Không tìm thấy tuyến đường")));
        }
        if (request.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> BusinessException.notFound("Không tìm thấy xe"));
            if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
                throw BusinessException.badRequest("Xe không hoạt động");
            }
            trip.setVehicle(vehicle);
            trip.setTotalSeats(vehicle.getSeatCount());
            trip.setAvailableSeats(vehicle.getSeatCount());
        }
        if (request.getDriverId() != null) trip.setDriver(findDriver(request.getDriverId()));
        if (request.getCoDriverId() != null) trip.setCoDriver(findDriver(request.getCoDriverId()));
        if (request.getDepartureTime() != null) trip.setDepartureTime(request.getDepartureTime());
        if (request.getArrivalTime() != null) trip.setArrivalTime(request.getArrivalTime());
        validateTimes(trip.getDepartureTime(), trip.getArrivalTime());
        if (request.getBasePrice() != null) trip.setBasePrice(request.getBasePrice());
        if (request.getNotes() != null) trip.setNotes(request.getNotes());
        return toResponse(trip);
    }

    public void delete(UUID id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy chuyến xe"));
        tripRepository.delete(trip);
    }

    private User findDriver(UUID id) {
        if (id == null) return null;
        User user = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy tài xế"));
        if (user.getRole() != UserRole.DRIVER) {
            throw BusinessException.badRequest("Người dùng được chọn không có vai trò tài xế");
        }
        return user;
    }

    private void validateTimes(java.time.LocalDateTime departure, java.time.LocalDateTime arrival) {
        if (!departure.isBefore(arrival)) {
            throw BusinessException.badRequest("Giờ khởi hành phải trước giờ đến");
        }
    }

    @Transactional(readOnly = true)
    public List<TripResponse> search(String from, String to, String date) {
        return tripRepository.findAllByOrderByDepartureTimeAsc().stream()
                .filter(t -> from == null || from.isBlank() || t.getRoute().getDepartureProvince().toLowerCase().contains(from.toLowerCase()))
                .filter(t -> to == null || to.isBlank() || t.getRoute().getArrivalProvince().toLowerCase().contains(to.toLowerCase()))
                .filter(t -> date == null || date.isBlank() || t.getDepartureTime().toLocalDate().toString().equals(date))
                .map(this::toResponse).toList();
    }

    private TripResponse toResponse(Trip trip) {
        return TripResponse.builder()
                .id(trip.getId())
                .tripCode(trip.getTripCode())
                .routeId(trip.getRoute().getId())
                .routeName(trip.getRoute().getName())
                .vehicleId(trip.getVehicle().getId())
                .licensePlate(trip.getVehicle().getLicensePlate())
                .driverId(trip.getDriver() == null ? null : trip.getDriver().getId())
                .driverName(trip.getDriver() == null ? null : trip.getDriver().getFullName())
                .coDriverId(trip.getCoDriver() == null ? null : trip.getCoDriver().getId())
                .coDriverName(trip.getCoDriver() == null ? null : trip.getCoDriver().getFullName())
                .departureTime(trip.getDepartureTime())
                .arrivalTime(trip.getArrivalTime())
                .basePrice(trip.getBasePrice())
                .status(trip.getStatus().name())
                .totalSeats(trip.getTotalSeats())
                .notes(trip.getNotes())
                .build();
    }
}
