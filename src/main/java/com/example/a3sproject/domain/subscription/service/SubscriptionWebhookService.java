package com.example.a3sproject.domain.subscription.service;

import com.example.a3sproject.domain.subscription.entity.SubscriptionBilling;
import com.example.a3sproject.domain.subscription.repository.SubscriptionBillingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionWebhookService {

    private final SubscriptionBillingRepository billingRepository;

    @Transactional
    public void handleSubscriptionPayment(String paymentId) {

        SubscriptionBilling billing = null;
        int retries = 5;
        while (retries-- > 0) {
            billing = billingRepository.findByPaymentId(paymentId).orElse(null);
            if (billing != null) {
                break;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (billing == null) {
            log.warn("구독 결제 웹훅 매칭 실패 - paymentId: {}", paymentId);
            throw new RuntimeException("빌링 정보 없음");
        }

        billing.markAsPaid(); // 상태 변경

        // 필요하면 subscription 기간 연장
//        billing.getSubscription().renewPeriod();
    }
}