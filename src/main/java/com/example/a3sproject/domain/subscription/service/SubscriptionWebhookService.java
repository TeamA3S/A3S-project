package com.example.a3sproject.domain.subscription.service;

import com.example.a3sproject.domain.subscription.entity.SubscriptionBilling;
import com.example.a3sproject.domain.subscription.repository.SubscriptionBillingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionWebhookService {

    private final SubscriptionBillingRepository billingRepository;

    @Transactional
    public void handleSubscriptionPayment(String paymentId) {

        SubscriptionBilling billing = (SubscriptionBilling) billingRepository
                .findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("빌링 정보 없음"));

        billing.markAsPaid(); // 상태 변경

        // 필요하면 subscription 기간 연장
//        billing.getSubscription().renewPeriod();
    }
}