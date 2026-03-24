package com.example.a3sproject.domain.portone.dto.response;

import com.example.a3sproject.domain.portone.enums.PortOnePayStatus;

import java.time.OffsetDateTime;

public record PortOnePaymentResponse (
        String id,
        PortOnePayStatus status,
        PaymentAmount amount,
        OffsetDateTime paidAt
) {
    public record PaymentAmount(
            int total,
            int discount
    ) {}
}
