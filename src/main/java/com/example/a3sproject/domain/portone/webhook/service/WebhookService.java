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
            JsonNode root = objectMapper.readTree(rawBody);
            
            // 1. 데이터 추출 (V2 규격: data 객체 내부 확인)
            JsonNode dataNode = root.path("data");
            String portOneId = dataNode.path("paymentId").asText(null);
            String webhookUuid = dataNode.path("transactionId").asText(null);
            
            // 만약 transactionId가 없으면 billingKey라도 식별자로 사용
            if (webhookUuid == null || webhookUuid.isBlank()) {
                webhookUuid = dataNode.path("billingKey").asText("UNKNOWN-" + System.currentTimeMillis());
            }
            
            String eventStatus = readEventStatus(root);

            log.info("웹훅 수신 프로세스 시작 - ID: {}, Status: {}", portOneId, eventStatus);

            // 2. 멱등성 체크 (중복 검증)
            if (webhookRepository.existsByWebhookUuid(webhookUuid)) {
                log.info("이미 처리된 웹훅이므로 스킵합니다: {}", webhookUuid);
                return;
            }

            // 3. Webhook 엔티티 생성 및 저장
            webhook = new Webhook(webhookUuid, portOneId, eventStatus);
            webhookRepository.save(webhook);

            // 4. 결제 확정 처리 분기 (핵심: SUB- 또는 BIH-로 시작하면 일반 결제 로직 스킵)
            if (portOneId != null && (portOneId.startsWith("SUB-") || portOneId.startsWith("BIH-"))) {
                log.info("구독 관련 결제(SUB/BIH) 웹훅입니다. 기록 후 종료합니다: {}", portOneId);
            } else if (portOneId != null && !portOneId.isBlank()) {
                log.info("일반 결제 웹훅 확정 프로세스 진행: {}", portOneId);
                paymentService.confirmPayment(portOneId, null);
            }

            // 5. 성공 상태 변경
            webhook.processedWebhook();
            
        } catch (Exception e) {
            log.error("웹훅 처리 중 에러 발생: {}", e.getMessage());
            if (webhook != null) {
                webhook.failedWebhook();
            }
            // 웹훅은 롤백을 전파하지 않도록 처리
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
