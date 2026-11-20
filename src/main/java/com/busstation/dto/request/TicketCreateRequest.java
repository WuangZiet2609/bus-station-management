package com.busstation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class TicketCreateRequest {
    @NotNull
    private UUID tripId;
    @NotNull
    private Integer seatNumber;
    private java.util.UUID customerId;
}
