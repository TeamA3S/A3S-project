package com.example.a3sproject.domain.payment.dto;

import com.example.a3sproject.domain.payment.entity.Payment;

// 사전 검증 결과를 담는 record
public record PaymentPrepareResult(
        Payment payment,
        boolean pointUsed,
        Long userId
) {}
