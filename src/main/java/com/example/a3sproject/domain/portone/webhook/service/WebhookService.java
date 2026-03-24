package com.example.a3sproject.domain.portone.webhook.service;

import com.example.a3sproject.domain.payment.service.PaymentService;
import com.example.a3sproject.domain.portone.webhook.entity.Webhook;
import com.example.a3sproject.domain.portone.webhook.repository.WebhookRepository;
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

    @Transactional
    public void handleWebhook(String rawBody) {
        Webhook webhook = null;
        try {
            JsonNode root = readJson(rawBody);
            String portOneId = root.path("data").path("paymentId").asText(null);
            String eventStatus = readEventStatus(root);
            String webhookUuid = root.path("data").path("transactionId").asText(null);

            if (webhookUuid == null) {
                webhookUuid = root.path("data").path("billingKey").asText(null);
            }

            // 1. 중복 검증
            if (webhookUuid != null && webhookRepository.existsByWebhookUuid(webhookUuid)) {
                log.info("이미 처리된 웹훅입니다: {}", webhookUuid);
                return;
            }

            // 2. Webhook 엔티티 생성 및 저장
            webhook = new Webhook(webhookUuid, portOneId, eventStatus);
            webhookRepository.save(webhook);

            // 3. 결제 확정 처리 (구독 결제 분기 처리)
            if (portOneId != null && ((portOneId.startsWith("SUB-") || portOneId.startsWith("BIH-")))) {
                log.info("구독 결제 웹훅은 별도 처리 없이 기록만 합니다: {}", portOneId);
                // 구독 결제는 SubscriptionService에서 직접 API 응답으로 처리하므로 여기서는 기록만 하고 종료
            } else if (portOneId != null) {
                paymentService.confirmPayment(portOneId, null);
            }

            // 4. 성공 상태 변경
            webhook.processedWebhook();
        } catch (Exception e) {
            log.error("웹훅 내부 처리 중 에러 발생: {}", e.getMessage());
            if (webhook != null) {
                webhook.failedWebhook();
            }
            // 웹훅은 롤백을 전파하지 않도록 예외를 밖으로 던지지 않거나 별도 처리
        }
    }

    private JsonNode readJson(String rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception e) {
            throw new PortOneException(ErrorCode.INVALID_WEBHOOK_JSON);
        }
    }

    private String readEventStatus(JsonNode root) {
        String nestedStatus = root.path("data").path("status").asText(null);
        if (nestedStatus != null && !nestedStatus.isBlank()) {
            return nestedStatus;
        }
        return root.path("status").asText(null);
    }
}
