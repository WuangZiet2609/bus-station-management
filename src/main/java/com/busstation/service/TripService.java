package com.busstation.service;

import com.busstation.dto.request.TripCreateRequest;
import com.busstation.dto.request.TripUpdateRequest;
import com.busstation.dto.response.TripResponse;
import com.busstation.entity.*;
import com.busstation.exception.BusinessException;
import com.busstation.repository.RouteRepository;
import com.busstation.repository.TicketRepository;
import org.springframework.transaction.annotation.Isolation;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import com.busstation.repository.TripRepository;
import com.busstation.repository.UserRepository;
import com.busstation.repository.VehicleRepository;
import com.busstation.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TripService {
    private final TripRepository tripRepository;
    private final TicketRepository ticketRepository;
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

    @Transactional(readOnly = true)
    public List<Integer> occupiedSeats(UUID id) {
        if (!tripRepository.existsById(id)) {
            throw BusinessException.notFound("Không tìm thấy chuyến xe");
        }
        return ticketRepository.findOccupiedSeats(id);
    }

    @Transactional(readOnly = true)
    public TripResponse findByTripCode(String tripCode) {
        return toResponse(tripRepository.findByTripCode(tripCode)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy chuyến xe")));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TripResponse create(TripCreateRequest request) {
        String tripCode = request.getTripCode();
        if (tripCode == null || tripCode.isBlank()) {
            tripCode = CodeGenerator.generate("TRIP", tripRepository.count());
        }
        if (tripRepository.existsByTripCode(tripCode)) {
            throw BusinessException.conflict("Mã chuyến đã tồn tại");
        }
        validateTimes(request.getDepartureTime(), request.getArrivalTime());
        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy tuyến đường"));
        Vehicle vehicle = vehicleRepository.findLockedById(request.getVehicleId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy xe"));

        Trip trip = Trip.builder()
                .tripCode(tripCode)
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
        validateSchedule(trip);
        return toResponse(tripRepository.save(trip));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TripResponse update(UUID id, TripUpdateRequest request) {
        Trip trip = tripRepository.findLockedById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy chuyến xe"));
        long booked = ticketRepository.countByTripId(id);
        if (!trip.getDepartureTime().isAfter(LocalDateTime.now())) {
            throw BusinessException.conflict("Chuyến đã đến giờ khởi hành, không thể chỉnh sửa");
        }
        if (booked > 0 && request.getRouteId() != null && !request.getRouteId().equals(trip.getRoute().getId())) {
            throw BusinessException.conflict("Không thể đổi tuyến của chuyến đã bán vé");
        }
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
            Vehicle vehicle = vehicleRepository.findLockedById(request.getVehicleId())
                    .orElseThrow(() -> BusinessException.notFound("Không tìm thấy xe"));
            if (vehicle.getSeatCount() < booked
                    || ticketRepository.existsByTripIdAndSeatNumberGreaterThan(id, vehicle.getSeatCount())) {
                throw BusinessException.conflict("Xe thay thế không đủ ghế cho các vé đã đặt");
            }
            trip.setVehicle(vehicle);
            trip.setTotalSeats(vehicle.getSeatCount());
            trip.setAvailableSeats(vehicle.getSeatCount() - Math.toIntExact(booked));
        }
        if (request.getDriverId() != null) trip.setDriver(findDriver(request.getDriverId()));
        if (request.getCoDriverId() != null) trip.setCoDriver(findDriver(request.getCoDriverId()));
        if (request.getDepartureTime() != null) trip.setDepartureTime(request.getDepartureTime());
        if (request.getArrivalTime() != null) trip.setArrivalTime(request.getArrivalTime());
        validateSchedule(trip);
        if (request.getBasePrice() != null) trip.setBasePrice(request.getBasePrice());
        if (request.getNotes() != null) trip.setNotes(request.getNotes());
        return toResponse(trip);
    }

    @Transactional
    public void delete(UUID id) {
        Trip trip = tripRepository.findLockedById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy chuyến xe"));
        if (ticketRepository.existsByTripId(id)) {
            throw BusinessException.conflict("Không thể xóa chuyến đã phát hành vé");
        }
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

    private void validateSchedule(Trip trip) {
        validateTimes(trip.getDepartureTime(), trip.getArrivalTime());
        if (!trip.getDepartureTime().isAfter(LocalDateTime.now())) {
            throw BusinessException.badRequest("Giờ khởi hành phải ở tương lai");
        }
        if (!Boolean.TRUE.equals(trip.getRoute().getIsActive())) {
            throw BusinessException.conflict("Tuyến đường không hoạt động");
        }
        UUID vehicleId = trip.getVehicle().getId();
        Vehicle vehicle = vehicleRepository.findLockedById(vehicleId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy xe"));
        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw BusinessException.conflict("Xe không hoạt động");
        }
        List<UUID> drivers = Stream.of(trip.getDriver(), trip.getCoDriver())
                .filter(java.util.Objects::nonNull).map(User::getId).sorted().toList();
        if (drivers.size() == 2 && drivers.get(0).equals(drivers.get(1))) {
            throw BusinessException.badRequest("Tài xế chính và tài xế phụ phải khác nhau");
        }
        for (UUID driverId : drivers) {
            User driver = userRepository.findLockedById(driverId)
                    .orElseThrow(() -> BusinessException.notFound("Không tìm thấy tài xế"));
            if (driver.getStatus() != UserStatus.ACTIVE) {
                throw BusinessException.conflict("Tài xế không hoạt động");
            }
        }
        if (tripRepository.hasScheduleConflict(trip.getId(), vehicleId, drivers,
                trip.getDepartureTime(), trip.getArrivalTime(), List.of(TripStatus.CANCELLED, TripStatus.COMPLETED))) {
            throw BusinessException.conflict("Xe hoặc tài xế đã có lịch trong khung giờ này");
        }
    }

    private void validateTimes(LocalDateTime departure, LocalDateTime arrival) {
        if (!departure.isBefore(arrival)) {
            throw BusinessException.badRequest("Giờ khởi hành phải trước giờ đến");
        }
    }

    @Transactional(readOnly = true)
    public List<TripResponse> search(String from, String to, String date) {
        return tripRepository.findAllByOrderByDepartureTimeAsc().stream()
                .filter(Trip::isOpenForBooking)
                .filter(t -> t.getAvailableSeats() > 0)
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
                .availableSeats(trip.getAvailableSeats())
                .notes(trip.getNotes())
                .build();
    }
}
