package com.example.a3sproject.domain.portone;

import com.example.a3sproject.domain.portone.dto.PortOneCancelPaymentRequest;
import com.example.a3sproject.domain.portone.dto.PortOneCancelPaymentResponse;
import com.example.a3sproject.domain.portone.dto.PortOnePaymentResponse;
import com.example.a3sproject.domain.portone.dto.ValidateBillingKeyResponse;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.PaymentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class PortOneClient { // 실제 API 호출, webhook이랑 같은 로직

    private final RestClient portOneRestClient; // @Qualifier 필요할 수 있음

    @Value("${portone.store.id}")
    private String storeId;

    public PortOneClient(
            @Value("${portone.api.base-url}") String baseUrl,
            @Value("${portone.api.secret}") String apiSecret
    ) {
        this.portOneRestClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "PortOne " + apiSecret)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public PortOnePaymentResponse getPayment(String paymentId) {
        return portOneRestClient
                .get()
                .uri("/payments/{paymentId}?storeId="+storeId, paymentId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new PaymentException(ErrorCode.PAYMENT_PORTONE_ERROR);
                }) // 400번대 에러
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new PaymentException(ErrorCode.PAYMENT_PORTONE_ERROR);
                }) // 500번대 에러
                .body(PortOnePaymentResponse.class);
    }

    // 결제 취소
    public PortOneCancelPaymentResponse cancelPayment(
            String paymentId, PortOneCancelPaymentRequest cancelRequest
    ){
        return portOneRestClient
                .post()
                .uri("/payments/{paymentId}/cancel", paymentId)
                .body(cancelRequest)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new PaymentException(ErrorCode.PAYMENT_PORTONE_ERROR);
                }) // 400번대 에러
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new PaymentException(ErrorCode.PAYMENT_PORTONE_ERROR);
                }) // 500번대 에러
                .body(PortOneCancelPaymentResponse.class);
    }

    // billingKey 유효성 검증
    public ValidateBillingKeyResponse getBillingKey(String billingKey) {
        return portOneRestClient
                .get()
                .uri("/billing-keys/{billingKey}"+storeId, billingKey)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new PaymentException(ErrorCode.PAYMENT_PORTONE_ERROR);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new PaymentException(ErrorCode.PAYMENT_PORTONE_ERROR);
                })
                .body(ValidateBillingKeyResponse.class);
    }
}
