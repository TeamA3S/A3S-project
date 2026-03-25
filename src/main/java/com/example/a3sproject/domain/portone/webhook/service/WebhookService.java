package com.example.a3sproject.domain.portone.webhook.service;

import com.example.a3sproject.domain.payment.service.PaymentService;
import com.example.a3sproject.domain.portone.webhook.entity.Webhook;
import com.example.a3sproject.domain.portone.webhook.enums.WebhookStatus;
import com.example.a3sproject.domain.portone.webhook.repository.WebhookRepository;
import com.example.a3sproject.domain.subscription.service.SubscriptionWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

//    @Transactional
    public void handleWebhook(String rawBody) {
        Webhook webhook = null;
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            log.info("====== rawBody 확인 {} ======", rawBody);
            String eventType = root.path("type").asText(""); // 이벤트 타입 추출
            log.info("====== type 확인 용 {} =======", eventType);

            JsonNode dataNode = root.path("data");
            log.info("====== dataNode 확인 {} =======", dataNode);
            String portOneId = dataNode.path("paymentId").asText(null);
            log.info("===== portOneId 확인 {} =====", portOneId);
            String webhookUuid = dataNode.path("transactionId").asText(null);
            log.info("===== webhookUuid 확인 {} =====", webhookUuid);

            // 1. 빌링키 관련 이벤트는 transactionId가 없으므로 billingKey를 ID로 사용
            if (webhookUuid == null || webhookUuid.isBlank()) {
                log.info("==== webhookUuid 검증 {} ======", webhookUuid);
                webhookUuid = dataNode.path("billingKey").asText("UNKNOWN-" + System.currentTimeMillis());
            }

            String eventStatus = readEventStatus(root);
            log.info("===== eventStatus {} =====", eventStatus);

            // 2. 멱등성 체크
            if (webhookRepository.existsByWebhookUuidAndStatus(webhookUuid, WebhookStatus.PROCESSED)) {
                log.info("===== webhookUuid 확인 {} =====", webhookUuid);
                return;
            }

            // 3. Webhook 기록 조회 / 생성
            String finalWebhookUuid = webhookUuid;
            webhook = webhookRepository.findByWebhookUuid(webhookUuid)
                    .orElseGet(() -> webhookRepository.save(new Webhook(finalWebhookUuid, portOneId, eventStatus)));

            // 4. 이벤트 타입별 분기 처리 (핵심 해결책)
            boolean processed = true;
            if (eventType.startsWith("BillingKey")) {
                log.info("=====빌링키 관련 웹훅 수신 - 기록 후 종료: {} =========", eventType);
            } else if ("Transaction.Paid".equals(eventType) && portOneId != null && portOneId.startsWith("PMN-")) {
                
                // 일반 결제(PMN- 등)만 PaymentService로 전달
                try {
                    log.info("========= 일반 결제 웹훅 수신: {}, {} ========", eventType, portOneId);
                    paymentService.confirmPayment(portOneId, null);
                } catch (Exception e) {
                    log.error("========= 일반 결제 웹훅 확정 실패(무시): {} ============", e.getMessage());
                    processed = false;
                }
            } else if ("Transaction.Paid".equals(eventType) && portOneId != null && portOneId.startsWith("SUB-")) {
                log.info("======= 구독 결제 웹훅 수신 - 기록 후 종료: {} ===========", portOneId);
                try {
                    // 🔥 구독 결제 성공 처리 로직 호출
                    log.info("========= 구독 결제 성공 처리 로직 호출: {}, {} ===========", eventType, portOneId);
                    subscriptionWebhookService.handleSubscriptionPayment(portOneId);
                } catch (Exception e) {
                    log.error("====== 구독 결제 처리 실패: {} =========", e.getMessage());
                    processed = false;
                }
            } else {
                log.info("====== 처리 대상이 아닌 웹훅 이벤트 - type: {}, paymentId: {} =======", eventType, portOneId);
            }
            if (processed) {
                webhook.processedWebhook();
            } else {
                webhook.failedWebhook();
            }
            webhookRepository.save(webhook);

        } catch (Exception e) {
            log.error("웹훅 처리 최종 에러: {}", e.getMessage());
        }
    }

    private String readEventStatus(JsonNode root) {
        String nestedStatus = root.path("data").path("status").asText(null);
        if (nestedStatus != null && !nestedStatus.isBlank()) {
            log.info("========== 이벤트 상태 read: {} ============", nestedStatus);
            return nestedStatus;
        }
        return root.path("status").asText("UNKNOWN");
    }
}
