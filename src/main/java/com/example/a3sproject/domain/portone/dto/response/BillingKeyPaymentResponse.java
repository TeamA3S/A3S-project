package com.example.a3sproject.domain.portone.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)  // ← 이거 추가!
public class BillingKeyPaymentResponse {

    @JsonProperty("payment")  // ← 이거 추가!
    private PaymentDetails payment;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)  // ← 이거 추가!
    public static class PaymentDetails {

        @JsonProperty("pgTxId")
        private String pgTxId;

        @JsonProperty("paidAt")
        private String paidAt;
    }
}
