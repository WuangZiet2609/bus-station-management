package com.busstation.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCreateRequest {

    @NotBlank(message = "Biển số xe không được để trống")
    @Pattern(regexp = "^[0-9]{2}[A-Z]-[0-9]{4,5}$", message = "Biển số xe không hợp lệ")
    private String licensePlate;

    @Size(max = 255, message = "Tên xe tối đa 255 ký tự")
    private String name;

    @Size(max = 100, message = "Hãng xe tối đa 100 ký tự")
    private String brand;

    @Size(max = 100, message = "Model xe tối đa 100 ký tự")
    private String model;

    @Min(value = 1990, message = "Năm sản xuất không hợp lệ")
    @Max(value = 2030, message = "Năm sản xuất không hợp lệ")
    private Integer yearOfManufacture;

    @NotNull(message = "Số ghế không được để trống")
    @Min(value = 1, message = "Số ghế phải lớn hơn 0")
    @Max(value = 60, message = "Số ghế không được quá 60")
    private Integer seatCount;

    private Map<String, Object> seatLayout;

    @Size(max = 50, message = "Loại xe tối đa 50 ký tự")
    private String vehicleType;

    @Size(max = 50, message = "Loại nhiên liệu tối đa 50 ký tự")
    private String fuelType;

    private LocalDate registrationDate;

    private LocalDate insuranceExpiryDate;

    private LocalDate inspectionExpiryDate;

    private List<String> imageUrls;

    private String notes;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleUpdateRequest {

    private String name;

    private String brand;

    private String model;

    private Integer yearOfManufacture;

    private Integer seatCount;

    private Map<String, Object> seatLayout;

    private String vehicleType;

    private String fuelType;

    private String status;

    private LocalDate insuranceExpiryDate;

    private LocalDate inspectionExpiryDate;

    private List<String> imageUrls;

    private String notes;
}