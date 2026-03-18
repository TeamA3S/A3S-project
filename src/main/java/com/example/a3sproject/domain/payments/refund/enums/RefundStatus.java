package com.example.a3sproject.domain.payments.refund.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RefundStatus {
    Request("환불 요청"),
    Completed("환불 완료"),
    Failed("환불 실패");

    private final String title;
}
