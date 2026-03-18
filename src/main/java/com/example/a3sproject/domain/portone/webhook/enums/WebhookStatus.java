package com.example.a3sproject.domain.portone.webhook.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WebhookStatus{
    RECEIVED("수신"),
    PROCESSED("완료"),
    FAILED("실패");

    private final String title;
}
