package com.busstation.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class TicketResponse {
    private UUID id;
    private String ticketCode;
    private UUID tripId;
    private UUID userId;
    private Integer seatNumber;
    private BigDecimal price;
    private String status;
    private String paymentStatus;
}
