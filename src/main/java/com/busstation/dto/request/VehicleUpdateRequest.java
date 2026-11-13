package com.busstation.dto.request;

import lombok.Data;

@Data
public class VehicleUpdateRequest {
    private String licensePlate;
    private String name;
    private String brand;
    private String model;
    private Integer seatCount;
    private String status;
}
