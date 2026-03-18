package com.example.a3sproject.domain.portone.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PortOnePayStatus {
    READY("결제 대기"),
    IN_PROGRESS("결제 중"),
    PAID("결제 성공"),
    FAILED("결제 실패"),
    CANCELLED("환불 완료"),
    PARTIAL_CANCELLED("부분 환불");

    private final String title;

}
