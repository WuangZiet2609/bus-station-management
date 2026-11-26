package com.busstation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TripUpdateRequest {
    @Size(min = 1, max = 50)
    private String tripCode;
    private UUID routeId;
    private UUID vehicleId;
    private UUID driverId;
    private UUID coDriverId;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal basePrice;
    private String notes;
}
