package com.example.a3sproject.domain.portone;

import com.example.a3sproject.domain.portone.dto.PortOneCancelPaymentRequest;
import com.example.a3sproject.domain.portone.dto.PortOneCancelPaymentResponse;
import com.example.a3sproject.domain.portone.dto.PortOnePaymentResponse;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class PortOneClient { // 실제 API 호출, webhook이랑 같은 로직

    private final RestClient portOneRestClient; // @Qualifier 필요할 수 있음

    public PortOnePaymentResponse getPayment(String portOneId) {
        return portOneRestClient
                .get()
                .uri("/payments/{paymentId}", portOneId)
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
            String paymentUuid, PortOneCancelPaymentRequest cancelRequest
    ){
        return portOneRestClient
                .post()
                .uri("/payments/{paymentId}/cancel", paymentUuid)
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
