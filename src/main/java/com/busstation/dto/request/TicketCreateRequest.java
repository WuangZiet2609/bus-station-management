package com.busstation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.UUID;

@Data
public class TicketCreateRequest {
    @NotNull
    private UUID tripId;
    @NotNull
    @Min(1)
    private Integer seatNumber;
    private java.util.UUID customerId;
}
