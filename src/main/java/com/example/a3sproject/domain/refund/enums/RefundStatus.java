package com.example.a3sproject.domain.refund.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RefundStatus {
    REQUEST("환불 요청"),
    COMPLETED("환불 완료"),
    FAILED("환불 실패");

    private final String title;
}
