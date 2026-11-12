package com.busstation.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private UUID id;
    private String bookingCode;
    private UserSummary user;
    private TripSummary trip;
    private List<SeatResponse> seats;
    private String passengerName;
    private String passengerPhone;
    private String passengerEmail;
    private String passengerIdentityCard;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String status;
    private String paymentMethod;
    private String paymentStatus;
    private String paymentTransactionId;
    private LocalDateTime paidAt;
    private LocalDateTime expiresAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSummary {

    private UUID id;
    private String tripCode;
    private RouteSummary route;
    private VehicleSummary vehicle;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String status;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID id;
    private UUID bookingId;
    private String transactionId;
    private String paymentMethod;
    private BigDecimal amount;
    private String status;
    private String gatewayTransactionId;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private BigDecimal refundAmount;
    private String refundReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitResponse {

    private String paymentUrl;
    private String transactionId;
    private String qrCode;
    private String deeplink;
}