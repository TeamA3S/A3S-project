package com.example.a3sproject.domain.payment.enums;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaidStatus {
    PENDING("결제 대기"),
    PROCESSING("결제 중"),
    SUCCESS("결제 성공"),
    FAILED("결제 실패"),
    REFUNDED("환불 완료");

    private final String title;
}