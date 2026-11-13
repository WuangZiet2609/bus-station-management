package com.busstation.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TripUpdateRequest {
    private String tripCode;
    private UUID routeId;
    private UUID vehicleId;
    private UUID driverId;
    private UUID coDriverId;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private BigDecimal basePrice;
    private String notes;
}
