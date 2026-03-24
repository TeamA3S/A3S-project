package com.example.a3sproject.domain.portone.webhook.service;

import com.example.a3sproject.domain.payment.service.PaymentService;
import com.example.a3sproject.domain.portone.webhook.entity.Webhook;
import com.example.a3sproject.domain.portone.webhook.repository.WebhookRepository;
import com.example.a3sproject.domain.subscription.service.SubscriptionWebhookService;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.PortOneException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookRepository webhookRepository;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;
    private final SubscriptionWebhookService subscriptionWebhookService;

    @Transactional
    public void handleWebhook(String rawBody) {
        Webhook webhook = null;
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String eventType = root.path("type").asText(""); // 이벤트 타입 추출
            
            JsonNode dataNode = root.path("data");
            String portOneId = dataNode.path("paymentId").asText(null);
            String webhookUuid = dataNode.path("transactionId").asText(null);
            
            // 1. 빌링키 관련 이벤트는 transactionId가 없으므로 billingKey를 ID로 사용
            if (webhookUuid == null || webhookUuid.isBlank()) {
                webhookUuid = dataNode.path("billingKey").asText("UNKNOWN-" + System.currentTimeMillis());
            }
            
            String eventStatus = readEventStatus(root);

            // 2. 멱등성 체크
            if (webhookRepository.existsByWebhookUuid(webhookUuid)) {
                return;
            }

            // 3. Webhook 기록 저장
            webhook = new Webhook(webhookUuid, portOneId, eventStatus);
            webhookRepository.save(webhook);

            // 4. 이벤트 타입별 분기 처리 (핵심 해결책)
            if (eventType.startsWith("BillingKey")) {
                log.info("빌링키 관련 웹훅 수신 - 기록 후 종료: {}", eventType);
            } else if (portOneId != null && portOneId.startsWith("PMN-")) {
                // 일반 결제(PMN- 등)만 PaymentService로 전달
                try {
                    paymentService.confirmPayment(portOneId, null);
                } catch (Exception e) {
                    log.error("일반 결제 웹훅 확정 실패(무시): {}", e.getMessage());
                }
            } else if (portOneId != null && !portOneId.isBlank()) {
                log.info("구독 결제 웹훅 수신 - 기록 후 종료: {}", portOneId);
                try {
                    // 🔥 구독 결제 성공 처리 로직 호출
                    subscriptionWebhookService.handleSubscriptionPayment(portOneId);
                } catch (Exception e) {
                    log.error("구독 결제 처리 실패: {}", e.getMessage());
                }
            }

            webhook.processedWebhook();
            
        } catch (Exception e) {
            log.error("웹훅 처리 최종 에러: {}", e.getMessage());
            if (webhook != null) {
                webhook.failedWebhook();
            }
        }
    }

    private String readEventStatus(JsonNode root) {
        String nestedStatus = root.path("data").path("status").asText(null);
        if (nestedStatus != null && !nestedStatus.isBlank()) {
            return nestedStatus;
        }
        return root.path("status").asText("UNKNOWN");
    }
}
