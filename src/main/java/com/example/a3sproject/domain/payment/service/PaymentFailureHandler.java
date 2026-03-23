package com.example.a3sproject.domain.payment.service;


import com.example.a3sproject.config.PortOneProperties;
import com.example.a3sproject.domain.payment.dto.PaymentProcessResult;
import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.portone.PortOneClient;
import com.example.a3sproject.domain.portone.dto.PortOneCancelPaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class PaymentFailureHandler {
    private final PortOneClient portOneClient;
    private final PaymentRollback paymentRollback;
    private final PortOneProperties portOneProperties;

    // catch 로직 통합!
    public void handlePaymentFailure(
            Payment payment,
            PaymentProcessResult result,
            Long userId
    ) {
        if (result != null && result.portOneConfirmed()) {
            portOneClient.cancelPayment(
                    result.portOneId(),
                    new PortOneCancelPaymentRequest("내부 오류로 인한 자동 취소", portOneProperties.getStore().getId())
            );
        }
        if (payment != null) {
            paymentRollback.failPayment(payment); // REQUIRES_NEW로 별도 커밋
        }
    }
}
