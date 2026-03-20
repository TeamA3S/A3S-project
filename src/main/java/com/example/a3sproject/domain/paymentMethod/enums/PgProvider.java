package com.example.a3sproject.domain.paymentMethod.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PgProvider {
    TOSS_PAYMENTS("토스"),
    KG_INICIS("KG"),
    NHN_KCP("농협"),
    KAKAO_PAY("카카오"),
    NAVER_PAY("네이버");

    private final String provider;
}
