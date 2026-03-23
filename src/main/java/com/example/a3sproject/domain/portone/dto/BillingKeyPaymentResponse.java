package com.example.a3sproject.domain.portone.dto;

import lombok.Getter;

@Getter
public class BillingKeyPaymentResponse {

    // 포트원이 값을 넣어서 채워주기 때문에 final 제거
    private String paymentId;
    private String status;
    private int amount;
}
