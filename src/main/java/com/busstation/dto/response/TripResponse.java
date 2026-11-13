package com.busstation.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class TripResponse {
    UUID id;
    String tripCode;
    UUID routeId;
    String routeName;
    UUID vehicleId;
    String licensePlate;
    UUID driverId;
    String driverName;
    UUID coDriverId;
    String coDriverName;
    LocalDateTime departureTime;
    LocalDateTime arrivalTime;
    BigDecimal basePrice;
    String status;
    Integer totalSeats;
    String notes;
}
