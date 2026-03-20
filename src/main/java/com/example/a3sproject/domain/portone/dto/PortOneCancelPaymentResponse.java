package com.example.a3sproject.domain.portone.dto;

import java.time.OffsetDateTime;
import java.util.Optional;

public record PortOneCancelPaymentResponse(
        Cancellation cancellation
) {
    public record Cancellation(
            String status,
            int totalAmount,
            OffsetDateTime cancelledAt
    ) {

    }
}