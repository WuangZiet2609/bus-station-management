package com.busstation.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {

    private UUID id;
    private String licensePlate;
    private String name;
    private String brand;
    private String model;
    private Integer yearOfManufacture;
    private Integer seatCount;
    private Map<String, Object> seatLayout;
    private String vehicleType;
    private String fuelType;
    private String status;
    private LocalDate registrationDate;
    private LocalDate insuranceExpiryDate;
    private LocalDate inspectionExpiryDate;
    private List<String> imageUrls;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {

    private UUID id;
    private String name;
    private String code;
    private String departureProvince;
    private String departureDistrict;
    private String departureWard;
    private String departureAddress;
    private String arrivalProvince;
    private String arrivalDistrict;
    private String arrivalWard;
    private String arrivalAddress;
    private Double distanceKm;
    private Integer estimatedDurationMinutes;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftResponse {

    private UUID id;
    private UserSummary employee;
    private TripSummary trip;
    private LocalDate shiftDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String shiftType;
    private String status;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private String type;
    private String title;
    private String message;
    private String channel;
    private UUID referenceId;
    private String referenceType;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;
}