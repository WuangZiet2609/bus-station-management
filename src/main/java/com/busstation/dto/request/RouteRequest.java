package com.busstation.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteCreateRequest {

    @NotBlank(message = "Tên tuyến không được để trống")
    @Size(max = 255, message = "Tên tuyến tối đa 255 ký tự")
    private String name;

    @NotBlank(message = "Mã tuyến không được để trống")
    @Size(max = 50, message = "Mã tuyến tối đa 50 ký tự")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Mã tuyến chỉ chứa chữ hoa, số và dấu gạch ngang")
    private String code;

    @NotBlank(message = "Tỉnh đi không được để trống")
    @Size(max = 100, message = "Tỉnh đi tối đa 100 ký tự")
    private String departureProvince;

    @Size(max = 100, message = "Quận/huyện đi tối đa 100 ký tự")
    private String departureDistrict;

    @Size(max = 100, message = "Phường/xã đi tối đa 100 ký tự")
    private String departureWard;

    private String departureAddress;

    @NotBlank(message = "Tỉnh đến không được để trống")
    @Size(max = 100, message = "Tỉnh đến tối đa 100 ký tự")
    private String arrivalProvince;

    @Size(max = 100, message = "Quận/huyện đến tối đa 100 ký tự")
    private String arrivalDistrict;

    @Size(max = 100, message = "Phường/xã đến tối đa 100 ký tự")
    private String arrivalWard;

    private String arrivalAddress;

    @DecimalMin(value = "0.0", message = "Khoảng cách phải lớn hơn hoặc bằng 0")
    private Double distanceKm;

    @Min(value = 1, message = "Thời gian dự kiến phải lớn hơn 0")
    private Integer estimatedDurationMinutes;

    private String description;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteUpdateRequest {

    private String name;

    private String code;

    private String departureProvince;

    private String departureDistrict;

    private String departureWard;

    private String departureAddress;

    private String arrivalProvince;

    private String arrivalDistrict;

    private String arrivalWard;

    private String arrivalAddress;

    private Double distanceKm;

    private Integer estimatedDurationMinutes;

    private String description;

    private Boolean isActive;
}