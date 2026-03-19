package com.example.a3sproject.domain.portone.webhook.service;

import com.example.a3sproject.domain.payment.service.PaymentService;
import com.example.a3sproject.domain.portone.webhook.entity.Webhook;
import com.example.a3sproject.domain.portone.webhook.enums.WebhookStatus;
import com.example.a3sproject.domain.portone.webhook.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookRepository webhookRepository;
    private final PaymentService paymentService;

    @Transactional
    public void handleWebhook(String webhookUuid, String portOneId, String eventStatus) {
        Webhook webhook = null;
        try {
            // 1. 중복 검증
            if(webhookRepository.existsByWebhookUuidAndStatus(webhookUuid, WebhookStatus.PROCESSED)){
                return;
            }
            // 2. Webhook 엔티티 생성 및 저장
            webhook = new Webhook(webhookUuid, portOneId, eventStatus);
            webhookRepository.save(webhook);
            // 3. 결제 확정 처리
            paymentService.confirmPayment(portOneId, null);
            // 4. 성공 상태 변경
            webhook.processedWebhook();
        }catch (Exception e) {
            // 5. 실패 상태 변경
            if (webhook != null) {
                webhook.failedWebhook();
            }
        }
    }
}