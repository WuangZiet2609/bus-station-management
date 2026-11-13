package com.busstation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VehicleCreateRequest {
    @NotBlank
    private String licensePlate;
    private String name;
    private String brand;
    private String model;
    @NotNull
    @Min(1)
    private Integer seatCount;
}
