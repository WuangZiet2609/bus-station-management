package com.busstation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VehicleUpdateRequest {
    @Pattern(regexp = ".*\\S.*")
    @Size(max = 20)
    private String licensePlate;
    private String name;
    private String brand;
    private String model;
    @Min(1)
    private Integer seatCount;
    private String status;
}
