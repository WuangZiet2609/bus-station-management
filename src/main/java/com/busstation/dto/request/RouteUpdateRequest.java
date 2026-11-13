package com.busstation.dto.request;

import lombok.Data;

@Data
public class RouteUpdateRequest {
    private String name;
    private String code;
    private String departureProvince;
    private String arrivalProvince;
    private Double distanceKm;
    private Integer estimatedDurationMinutes;
    private Boolean isActive;
}
