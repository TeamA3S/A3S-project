package com.example.a3sproject.domain.portone;

import com.example.a3sproject.domain.portone.dto.PortOneCancelPaymentRequest;
import com.example.a3sproject.domain.portone.dto.PortOneCancelPaymentResponse;
import com.example.a3sproject.domain.portone.dto.PortOnePaymentResponse;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

    public PortOnePaymentResponse getPayment(String portOneId) {
        return portOneRestClient
                .get()
                .uri("/payments/{paymentId}?storeId="+storeId, portOneId)
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
            String portOneId, PortOneCancelPaymentRequest cancelRequest
    ){
        return portOneRestClient
                .post()
                .uri("/payments/{paymentId}/cancel", portOneId)
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
}
