package com.example.a3sproject.domain.portone.dto.request;

public record PortOneCancelPaymentRequest(
        String reason,
        String storeId
) { }