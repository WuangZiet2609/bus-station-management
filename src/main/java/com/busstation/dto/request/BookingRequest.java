package com.busstation.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreateRequest {

    @NotNull(message = "Chuyến xe không được để trống")
    private UUID tripId;

    @NotEmpty(message = "Danh sách ghế không được để trống")
    @Size(max = 6, message = "Tối đa 6 ghế mỗi lần đặt")
    private List<UUID> seatIds;

    @NotBlank(message = "Tên hành khách không được để trống")
    @Size(max = 255, message = "Tên hành khách tối đa 255 ký tự")
    private String passengerName;

    @NotBlank(message = "Số điện thoại hành khách không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ")
    private String passengerPhone;

    @Email(message = "Email không hợp lệ")
    private String passengerEmail;

    @Pattern(regexp = "^[0-9]{9,12}$", message = "CCCD/CMND không hợp lệ")
    private String passengerIdentityCard;

    private String notes;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingUpdateRequest {

    private String status;

    private String cancelReason;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Đơn đặt vé không được để trống")
    private UUID bookingId;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;

    private String returnUrl;
}