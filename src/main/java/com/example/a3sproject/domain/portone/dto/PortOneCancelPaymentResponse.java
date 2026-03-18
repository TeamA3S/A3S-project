package com.example.a3sproject.domain.portone.dto;

import java.time.OffsetDateTime;
import java.util.Optional;

public record PortOneCancelPaymentResponse(
        String status,
        int totalAmount,
        OffsetDateTime cancelledAt
) { }