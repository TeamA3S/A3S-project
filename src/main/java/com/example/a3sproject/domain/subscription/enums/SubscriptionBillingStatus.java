package com.example.a3sproject.domain.subscription.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubscriptionBillingStatus {
    COMPLETED("결제 완료"),
    FAILED("결제 실패"),
    PENDING("결제 대기");

    private final String title;
}
