package com.example.a3sproject.domain.refunds.dto.request;

public record RefundRequest(
        Long paymentId,
        String reason
) {}