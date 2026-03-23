package com.example.a3sproject.domain.portone.dto;

import com.example.a3sproject.domain.portone.enums.PortOnePayStatus;

import java.time.OffsetDateTime;

public record PortOneBillingKeyPaymentResponse(
        String id,
        PortOnePayStatus status,
        OffsetDateTime paidAt
) {}
