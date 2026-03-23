package com.example.a3sproject.domain.portone.dto.response;

import java.time.OffsetDateTime;

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