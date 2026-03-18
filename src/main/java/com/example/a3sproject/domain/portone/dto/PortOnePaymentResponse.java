package com.example.a3sproject.domain.portone.dto;

import com.example.a3sproject.domain.portone.enums.PortOnePayStatus;

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
