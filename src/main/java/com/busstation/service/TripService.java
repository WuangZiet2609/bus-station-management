package com.busstation.service;

import com.busstation.dto.request.TripCreateRequest;
import com.busstation.dto.request.TripSearchRequest;
import com.busstation.dto.request.TripUpdateRequest;
import com.busstation.dto.response.PageResponse;
import com.busstation.dto.response.TripResponse;
import com.busstation.dto.response.TripSeatsResponse;
import com.busstation.entity.Route;
import com.busstation.entity.Seat;
import com.busstation.entity.SeatStatus;
import com.busstation.entity.Trip;
import com.busstation.entity.TripStatus;
import com.busstation.entity.Vehicle;
import com.busstation.exception.BusinessException;
import com.busstation.repository.RouteRepository;
import com.busstation.repository.SeatRepository;
import com.busstation.repository.TripRepository;
import com.busstation.repository.UserRepository;
import com.busstation.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public TripResponse createTrip(TripCreateRequest request) {
        if (tripRepository.existsByTripCode(request.getTripCode())) {
            throw BusinessException.conflict("Mã chuyến đã tồn tại");
        }

        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy tuyến đường"));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy xe"));

        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw BusinessException.badRequest("Xe không ở trạng thái hoạt động");
        }

        if (request.getDriverId() != null) {
            userRepository.findById(request.getDriverId())
                    .orElseThrow(() -> BusinessException.notFound("Không tìm thấy tài xế"));
        }

        if (request.getCoDriverId() != null) {
            userRepository.findById(request.getCoDriverId())
                    .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phụ xe"));
        }

        if (request.getDepartureTime().isAfter(request.getArrivalTime())) {
            throw BusinessException.badRequest("Giờ khởi hành phải trước giờ đến");
        }

        Trip trip = Trip.builder()
                .tripCode(request.getTripCode())
                .route(route)
                .vehicle(vehicle)
                .driver(request.getDriverId() != null ? userRepository.getReferenceById(request.getDriverId()) : null)
                .coDriver(request.getCoDriverId() != null ? userRepository.getReferenceById(request.getCoDriverId()) : null)
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .basePrice(request.getBasePrice())
                .status(TripStatus.SCHEDULED)
                .availableSeats(vehicle.getSeatCount())
                .totalSeats(vehicle.getSeatCount())
                .notes(request.getNotes())
                .build();

        trip = tripRepository.save(trip);

        createSeatsForTrip(trip, vehicle);

        log.info("Trip created: {}", trip.getTripCode());
        return buildTripResponse(trip);
    }

    @Transactional(readOnly = true)
    public PageResponse<TripResponse> searchTrips(TripSearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(),
                Sort.by(Sort.Direction.fromString(request.getSortDir()), request.getSortBy()));

        List<TripStatus> statuses = request.getStatus() != null ?
                List.of(TripStatus.valueOf(request.getStatus().toUpperCase())) :
                List.of(TripStatus.SCHEDULED, TripStatus.BOARDING, TripStatus.IN_PROGRESS);

        Page<Trip> tripPage;
        if (request.getRouteId() != null && request.getDepartureDate() != null) {
            tripPage = tripRepository.findByRouteAndDate(
                    request.getRouteId(), request.getDepartureDate(), statuses, pageable);
        } else if (request.getDepartureTimeFrom() != null && request.getDepartureTimeTo() != null) {
            tripPage = tripRepository.findByDepartureTimeBetweenAndStatuses(
                    request.getDepartureTimeFrom(), request.getDepartureTimeTo(), statuses, pageable);
        } else {
            tripPage = tripRepository.findByStatus(statuses.get(0), pageable);
        }

        return PageResponse.<TripResponse>builder()
                .content(tripPage.map(this::buildTripResponse).getContent())
                .page(tripPage.getNumber())
                .size(tripPage.getSize())
                .totalElements(tripPage.getTotalElements())
                .totalPages(tripPage.getTotalPages())
                .first(tripPage.isFirst())
                .last(tripPage.isLast())
                .empty(tripPage.isEmpty())
                .build();
    }

    @Transactional(readOnly = true)
    public TripResponse getTripById(UUID id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy chuyến xe"));
        return buildTripResponse(trip);
    }

    @Transactional(readOnly = true)
    public TripSeatsResponse getTripSeats(UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy chuyến xe"));

        List<Seat> seats = seatRepository.findByTripId(tripId);
        long availableCount = seats.stream().filter(s -> s.getStatus() == SeatStatus.AVAILABLE).count();

        return TripSeatsResponse.builder()
                .tripId(trip.getId())
                .tripCode(trip.getTripCode())
                .vehicle(buildVehicleSummary(trip.getVehicle()))
                .totalSeats(trip.getTotalSeats())
                .availableSeats((int) availableCount)
                .seats(seats.stream().map(this::buildSeatResponse).collect(Collectors.toList()))
                .build();
    }

    @Transactional
    public TripResponse updateTrip(UUID id, TripUpdateRequest request) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy chuyến xe"));

        if (trip.getStatus() != TripStatus.SCHEDULED) {
            throw BusinessException.badRequest("Chỉ có thể cập nhật chuyến ở trạng thái đã lên lịch");
        }

        if (request.getRouteId() != null) {
            Route route = routeRepository.findById(request.getRouteId())
                    .orElseThrow(() -> BusinessException.notFound("Không tìm thấy tuyến đường"));
            trip.setRoute(route);
        }

        if (request.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> BusinessException.notFound("Không tìm thấy xe"));
            trip.setVehicle(vehicle);
            trip.setTotalSeats(vehicle.getSeatCount());
            trip.setAvailableSeats(vehicle.getSeatCount());
        }

        if (request.getDriverId() != null) {
            trip.setDriver(userRepository.getReferenceById(request.getDriverId()));
        }

        if (request.getCoDriverId() != null) {
            trip.setCoDriver(userRepository.getReferenceById(request.getCoDriverId()));
        }

        if (request.getDepartureTime() != null) {
            trip.setDepartureTime(request.getDepartureTime());
        }

        if (request.getArrivalTime() != null) {
            trip.setArrivalTime(request.getArrivalTime());
        }

        if (request.getBasePrice() != null) {
            trip.setBasePrice(request.getBasePrice());
        }

        if (request.getNotes() != null) {
            trip.setNotes(request.getNotes());
        }

        trip = tripRepository.save(trip);
        log.info("Trip updated: {}", trip.getTripCode());

        return buildTripResponse(trip);
    }

    @Transactional
    public void cancelTrip(UUID id, UUID cancelledBy, String reason) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy chuyến xe"));

        if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
            throw BusinessException.badRequest("Không thể hủy chuyến đã hoàn thành hoặc đã hủy");
        }

        trip.setStatus(TripStatus.CANCELLED);
        trip.setCancelReason(reason);
        trip.setCancelledAt(LocalDateTime.now());
        trip.setCancelledBy(userRepository.getReferenceById(cancelledBy));

        tripRepository.save(trip);
        log.info("Trip cancelled: {}", trip.getTripCode());
    }

    @Transactional
    public void startTrip(UUID id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy chuyến xe"));

        if (trip.getStatus() != TripStatus.SCHEDULED && trip.getStatus() != TripStatus.BOARDING) {
            throw BusinessException.badRequest("Chuyến xe không ở trạng thái có thể bắt đầu");
        }

        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setActualDepartureTime(LocalDateTime.now());
        tripRepository.save(trip);
    }

    @Transactional
    public void completeTrip(UUID id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy chuyến xe"));

        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw BusinessException.badRequest("Chuyến xe không ở trạng thái đang chạy");
        }

        trip.setStatus(TripStatus.COMPLETED);
        trip.setActualArrivalTime(LocalDateTime.now());
        tripRepository.save(trip);
    }

    private void createSeatsForTrip(Trip trip, Vehicle vehicle) {
        int seatCount = vehicle.getSeatCount();
        List<Seat> seats = new java.util.ArrayList<>();

        for (int i = 1; i <= seatCount; i++) {
            Seat seat = Seat.builder()
                    .trip(trip)
                    .seatNumber(String.valueOf(i))
                    .seatRow((i - 1) / 4 + 1)
                    .seatCol((i - 1) % 4 + 1)
                    .floor(1)
                    .seatType("STANDARD")
                    .priceMultiplier(1.0)
                    .status(SeatStatus.AVAILABLE)
                    .build();
            seats.add(seat);
        }

        seatRepository.saveAll(seats);
    }

    private TripResponse buildTripResponse(Trip trip) {
        return TripResponse.builder()
                .id(trip.getId())
                .tripCode(trip.getTripCode())
                .route(buildRouteSummary(trip.getRoute()))
                .vehicle(buildVehicleSummary(trip.getVehicle()))
                .driver(buildUserSummary(trip.getDriver()))
                .coDriver(buildUserSummary(trip.getCoDriver()))
                .departureTime(trip.getDepartureTime())
                .arrivalTime(trip.getArrivalTime())
                .actualDepartureTime(trip.getActualDepartureTime())
                .actualArrivalTime(trip.getActualArrivalTime())
                .basePrice(trip.getBasePrice())
                .status(trip.getStatus().name())
                .availableSeats(trip.getAvailableSeats())
                .totalSeats(trip.getTotalSeats())
                .notes(trip.getNotes())
                .cancelledAt(trip.getCancelledAt())
                .cancelledBy(buildUserSummary(trip.getCancelledBy()))
                .cancelReason(trip.getCancelReason())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .build();
    }

    private RouteSummary buildRouteSummary(Route route) {
        if (route == null) return null;
        return RouteSummary.builder()
                .id(route.getId())
                .name(route.getName())
                .code(route.getCode())
                .departureProvince(route.getDepartureProvince())
                .arrivalProvince(route.getArrivalProvince())
                .distanceKm(route.getDistanceKm())
                .estimatedDurationMinutes(route.getEstimatedDurationMinutes())
                .build();
    }

    private VehicleSummary buildVehicleSummary(Vehicle vehicle) {
        if (vehicle == null) return null;
        return VehicleSummary.builder()
                .id(vehicle.getId())
                .licensePlate(vehicle.getLicensePlate())
                .name(vehicle.getName())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .seatCount(vehicle.getSeatCount())
                .vehicleType(vehicle.getVehicleType())
                .status(vehicle.getStatus().name())
                .build();
    }

    private UserSummary buildUserSummary(com.busstation.entity.User user) {
        if (user == null) return null;
        return UserSummary.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    private SeatResponse buildSeatResponse(Seat seat) {
        return SeatResponse.builder()
                .id(seat.getId())
                .seatNumber(seat.getSeatNumber())
                .seatRow(seat.getSeatRow())
                .seatCol(seat.getSeatCol())
                .floor(seat.getFloor())
                .seatType(seat.getSeatType())
                .priceMultiplier(seat.getPriceMultiplier())
                .status(seat.getStatus().name())
                .build();
    }
}