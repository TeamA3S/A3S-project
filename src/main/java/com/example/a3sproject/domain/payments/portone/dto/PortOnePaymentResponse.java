package com.example.a3sproject.domain.payments.portone.dto;

import com.example.a3sproject.domain.payments.portone.enums.PortOnePayStatus;

import java.time.OffsetDateTime;

public record PortOnePaymentResponse (
        String id,
        PortOnePayStatus status,
        Amount amount,
        OffsetDateTime paidAt
) {
    public record Amount(
            int total
    ) {}
}
