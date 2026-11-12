package com.busstation.service;

import com.busstation.dto.request.ShiftCreateRequest;
import com.busstation.dto.request.ShiftSearchRequest;
import com.busstation.dto.request.ShiftUpdateRequest;
import com.busstation.dto.response.PageResponse;
import com.busstation.dto.response.ShiftResponse;
import com.busstation.entity.Shift;
import com.busstation.entity.ShiftStatus;
import com.busstation.entity.Trip;
import com.busstation.entity.User;
import com.busstation.exception.BusinessException;
import com.busstation.repository.ShiftRepository;
import com.busstation.repository.TripRepository;
import com.busstation.repository.UserRepository;
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
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;

    @Transactional
    public ShiftResponse createShift(ShiftCreateRequest request) {
        User employee = userRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy nhân viên"));

        if (request.getTripId() != null) {
            tripRepository.findById(request.getTripId())
                    .orElseThrow(() -> BusinessException.notFound("Không tìm thấy chuyến xe"));
        }

        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw BusinessException.badRequest("Giờ bắt đầu phải trước giờ kết thúc");
        }

        Shift shift = Shift.builder()
                .employee(employee)
                .trip(request.getTripId() != null ? tripRepository.getReferenceById(request.getTripId()) : null)
                .shiftDate(request.getShiftDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .shiftType(request.getShiftType())
                .status(ShiftStatus.SCHEDULED)
                .notes(request.getNotes())
                .build();

        shift = shiftRepository.save(shift);
        log.info("Shift created for employee: {}", employee.getEmail());

        return buildShiftResponse(shift);
    }

    @Transactional(readOnly = true)
    public PageResponse<ShiftResponse> getShifts(ShiftSearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by("shiftDate").descending());

        Page<Shift> shiftPage;
        if (request.getEmployeeId() != null && request.getDateFrom() != null && request.getDateTo() != null) {
            List<Shift> shifts = shiftRepository.findByEmployeeIdAndShiftDateBetween(
                    request.getEmployeeId(), request.getDateFrom(), request.getDateTo());
            shiftPage = new org.springframework.data.domain.PageImpl<>(shifts, pageable, shifts.size());
        } else if (request.getEmployeeId() != null) {
            shiftPage = shiftRepository.findByEmployeeId(request.getEmployeeId(), pageable);
        } else {
            shiftPage = shiftRepository.findAll(pageable);
        }

        return PageResponse.<ShiftResponse>builder()
                .content(shiftPage.map(this::buildShiftResponse).getContent())
                .page(shiftPage.getNumber())
                .size(shiftPage.getSize())
                .totalElements(shiftPage.getTotalElements())
                .totalPages(shiftPage.getTotalPages())
                .first(shiftPage.isFirst())
                .last(shiftPage.isLast())
                .empty(shiftPage.isEmpty())
                .build();
    }

    @Transactional(readOnly = true)
    public ShiftResponse getShiftById(UUID id) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy ca làm việc"));
        return buildShiftResponse(shift);
    }

    @Transactional
    public ShiftResponse updateShift(UUID id, ShiftUpdateRequest request) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy ca làm việc"));

        if (request.getTripId() != null) {
            shift.setTrip(tripRepository.getReferenceById(request.getTripId()));
        }
        if (request.getShiftDate() != null) shift.setShiftDate(request.getShiftDate());
        if (request.getStartTime() != null) shift.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) shift.setEndTime(request.getEndTime());
        if (request.getShiftType() != null) shift.setShiftType(request.getShiftType());
        if (request.getStatus() != null) shift.setStatus(ShiftStatus.valueOf(request.getStatus().toUpperCase()));
        if (request.getNotes() != null) shift.setNotes(request.getNotes());

        shift = shiftRepository.save(shift);
        return buildShiftResponse(shift);
    }

    @Transactional
    public ShiftResponse checkIn(UUID id) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy ca làm việc"));

        if (shift.getStatus() != ShiftStatus.SCHEDULED && shift.getStatus() != ShiftStatus.IN_PROGRESS) {
            throw BusinessException.badRequest("Không thể check-in ca này");
        }

        shift.setStatus(ShiftStatus.IN_PROGRESS);
        shift.setCheckInTime(LocalDateTime.now());
        shift = shiftRepository.save(shift);

        return buildShiftResponse(shift);
    }

    @Transactional
    public ShiftResponse checkOut(UUID id) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy ca làm việc"));

        if (shift.getStatus() != ShiftStatus.IN_PROGRESS) {
            throw BusinessException.badRequest("Ca làm việc chưa được check-in");
        }

        shift.setStatus(ShiftStatus.COMPLETED);
        shift.setCheckOutTime(LocalDateTime.now());
        shift = shiftRepository.save(shift);

        return buildShiftResponse(shift);
    }

    @Transactional
    public void deleteShift(UUID id) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy ca làm việc"));
        shiftRepository.delete(shift);
    }

    private ShiftResponse buildShiftResponse(Shift shift) {
        return ShiftResponse.builder()
                .id(shift.getId())
                .employee(buildUserSummary(shift.getEmployee()))
                .trip(buildTripSummary(shift.getTrip()))
                .shiftDate(shift.getShiftDate())
                .startTime(shift.getStartTime())
                .endTime(shift.getEndTime())
                .shiftType(shift.getShiftType())
                .status(shift.getStatus().name())
                .checkInTime(shift.getCheckInTime())
                .checkOutTime(shift.getCheckOutTime())
                .notes(shift.getNotes())
                .createdAt(shift.getCreatedAt())
                .updatedAt(shift.getUpdatedAt())
                .build();
    }

    private com.busstation.dto.response.UserSummary buildUserSummary(User user) {
        if (user == null) return null;
        return com.busstation.dto.response.UserSummary.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    private com.busstation.dto.response.TripSummary buildTripSummary(Trip trip) {
        if (trip == null) return null;
        return com.busstation.dto.response.TripSummary.builder()
                .id(trip.getId())
                .tripCode(trip.getTripCode())
                .route(buildRouteSummary(trip.getRoute()))
                .vehicle(buildVehicleSummary(trip.getVehicle()))
                .departureTime(trip.getDepartureTime())
                .arrivalTime(trip.getArrivalTime())
                .status(trip.getStatus().name())
                .build();
    }

    private com.busstation.dto.response.RouteSummary buildRouteSummary(com.busstation.entity.Route route) {
        if (route == null) return null;
        return com.busstation.dto.response.RouteSummary.builder()
                .id(route.getId())
                .name(route.getName())
                .code(route.getCode())
                .departureProvince(route.getDepartureProvince())
                .arrivalProvince(route.getArrivalProvince())
                .distanceKm(route.getDistanceKm())
                .estimatedDurationMinutes(route.getEstimatedDurationMinutes())
                .build();
    }

    private com.busstation.dto.response.VehicleSummary buildVehicleSummary(com.busstation.entity.Vehicle vehicle) {
        if (vehicle == null) return null;
        return com.busstation.dto.response.VehicleSummary.builder()
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
}