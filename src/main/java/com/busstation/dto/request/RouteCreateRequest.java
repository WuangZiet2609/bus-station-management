package com.busstation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RouteCreateRequest {
    @NotBlank
    private String name;
    private String code;
    @NotBlank
    private String departureProvince;
    @NotBlank
    private String arrivalProvince;
    private Double distanceKm;
    private Integer estimatedDurationMinutes;
}
