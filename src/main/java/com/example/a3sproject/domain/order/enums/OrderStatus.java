package com.example.a3sproject.domain.order.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {

    PENDING("결제 대기"),
    COMPLETED("주문 완료"),
    CANCELLED("주문 취소"),
    REFUNDED("환불");

    private final String status;
}
