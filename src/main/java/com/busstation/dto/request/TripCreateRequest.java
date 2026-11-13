package com.busstation.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TripCreateRequest {
    @NotBlank
    private String tripCode;
    @NotNull
    private UUID routeId;
    @NotNull
    private UUID vehicleId;
    private UUID driverId;
    private UUID coDriverId;
    @NotNull
    @Future
    private LocalDateTime departureTime;
    @NotNull
    @Future
    private LocalDateTime arrivalTime;
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal basePrice;
    private String notes;
}
