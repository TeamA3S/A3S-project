package com.example.a3sproject.domain.subscription.service;

import com.example.a3sproject.domain.subscription.entity.SubscriptionBilling;
import com.example.a3sproject.domain.subscription.repository.SubscriptionBillingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionWebhookServiceTest {

    @Mock private SubscriptionBillingRepository billingRepository;

    @InjectMocks
    private SubscriptionWebhookService subscriptionWebhookService;

    @Test
    @DisplayName("웹훅 처리 - paymentId로 빌링을 찾으면 markAsPaid()가 호출된다")
    void handleSubscriptionPayment_빌링존재_markAsPaid호출() {
        // given: paymentId에 해당하는 빌링 즉시 조회 성공
        SubscriptionBilling billing = mock(SubscriptionBilling.class);
        given(billingRepository.findByPaymentId("payment-001")).willReturn(Optional.of(billing));

        // when
        subscriptionWebhookService.handleSubscriptionPayment("payment-001");

        // then: 상태 변경 1회 호출
        verify(billing, times(1)).markAsPaid();
    }

    @Test
    @DisplayName("웹훅 처리 - 재시도 후에도 빌링을 찾지 못하면 RuntimeException이 발생한다")
    void handleSubscriptionPayment_빌링없음_예외발생() {
        // given: 모든 재시도에서 빌링 조회 실패 (재시도 5회)
        given(billingRepository.findByPaymentId("unknown-payment")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> subscriptionWebhookService.handleSubscriptionPayment("unknown-payment"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("빌링 정보 없음");

        // 5회 재시도 검증
        verify(billingRepository, times(5)).findByPaymentId("unknown-payment");
    }
}