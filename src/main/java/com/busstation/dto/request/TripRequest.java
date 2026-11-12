package com.busstation.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripCreateRequest {

    @NotBlank(message = "Mã chuyến không được để trống")
    @Size(max = 50, message = "Mã chuyến tối đa 50 ký tự")
    private String tripCode;

    @NotNull(message = "Tuyến đường không được để trống")
    private UUID routeId;

    @NotNull(message = "Xe không được để trống")
    private UUID vehicleId;

    private UUID driverId;

    private UUID coDriverId;

    @NotNull(message = "Giờ khởi hành không được để trống")
    @Future(message = "Giờ khởi hành phải ở tương lai")
    private LocalDateTime departureTime;

    @NotNull(message = "Giờ đến không được để trống")
    @Future(message = "Giờ đến phải ở tương lai")
    private LocalDateTime arrivalTime;

    @NotNull(message = "Giá vé không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá vé phải lớn hơn 0")
    private BigDecimal basePrice;

    private String notes;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripUpdateRequest {

    private UUID routeId;

    private UUID vehicleId;

    private UUID driverId;

    private UUID coDriverId;

    private LocalDateTime departureTime;

    private LocalDateTime arrivalTime;

    private BigDecimal basePrice;

    private String notes;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSearchRequest {

    private UUID routeId;

    private LocalDate departureDate;

    private LocalDateTime departureTimeFrom;

    private LocalDateTime departureTimeTo;

    private String status;

    private int page = 0;

    private int size = 20;

    private String sortBy = "departureTime";

    private String sortDir = "asc";
}