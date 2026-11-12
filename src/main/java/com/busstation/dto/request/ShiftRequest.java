package com.busstation.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftCreateRequest {

    @NotNull(message = "Nhân viên không được để trống")
    private UUID employeeId;

    private UUID tripId;

    @NotNull(message = "Ngày ca không được để trống")
    @FutureOrPresent(message = "Ngày ca phải hôm nay hoặc tương lai")
    private LocalDate shiftDate;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    private LocalTime endTime;

    @Size(max = 50, message = "Loại ca tối đa 50 ký tự")
    private String shiftType = "DRIVING";

    private String notes;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftUpdateRequest {

    private UUID tripId;

    private LocalDate shiftDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String shiftType;

    private String status;

    private String notes;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftSearchRequest {

    private UUID employeeId;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private String status;

    private int page = 0;

    private int size = 20;
}