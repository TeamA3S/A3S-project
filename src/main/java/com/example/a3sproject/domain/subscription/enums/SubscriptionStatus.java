package com.example.a3sproject.domain.subscription.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubscriptionStatus {
    ACTIVE("구독 활성화"),
    CANCELLED("구독 취소"),
    PAST_DUE("결제 연체(미납)"),
    TRIALING("지연"),
    ENDED("구독 종료");

    private final String title;
}
