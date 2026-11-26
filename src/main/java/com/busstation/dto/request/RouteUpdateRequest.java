package com.busstation.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class RouteUpdateRequest {
    private String name;
    private String code;
    private String departureProvince;
    private String arrivalProvince;
    @Positive
    private Double distanceKm;
    @Positive
    private Integer estimatedDurationMinutes;
    private Boolean isActive;
}
