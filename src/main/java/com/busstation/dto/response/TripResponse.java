package com.busstation.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {

    private UUID id;
    private String tripCode;
    private RouteSummary route;
    private VehicleSummary vehicle;
    private UserSummary driver;
    private UserSummary coDriver;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private LocalDateTime actualDepartureTime;
    private LocalDateTime actualArrivalTime;
    private BigDecimal basePrice;
    private String status;
    private Integer availableSeats;
    private Integer totalSeats;
    private String notes;
    private LocalDateTime cancelledAt;
    private UserSummary cancelledBy;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteSummary {

    private UUID id;
    private String name;
    private String code;
    private String departureProvince;
    private String arrivalProvince;
    private Double distanceKm;
    private Integer estimatedDurationMinutes;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSummary {

    private UUID id;
    private String licensePlate;
    private String name;
    private String brand;
    private String model;
    private Integer seatCount;
    private String vehicleType;
    private String status;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary {

    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {

    private UUID id;
    private String seatNumber;
    private Integer seatRow;
    private Integer seatCol;
    private Integer floor;
    private String seatType;
    private Double priceMultiplier;
    private String status;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSeatsResponse {

    private UUID tripId;
    private String tripCode;
    private VehicleSummary vehicle;
    private Integer totalSeats;
    private Integer availableSeats;
    private List<SeatResponse> seats;
}