package com.example.a3sproject.domain.portone.webhook.service;

import com.example.a3sproject.domain.payment.service.PaymentService;
import com.example.a3sproject.domain.portone.webhook.entity.Webhook;
import com.example.a3sproject.domain.portone.webhook.enums.WebhookStatus;
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
            String eventType = root.path("type").asText(null);
            String portOneId = root.path("data").path("paymentId").asText(null);
            String eventStatus = readEventStatus(root);
            String webhookUuid = root.path("data").path("transactionId").asText(null);



            // 1. 중복 검증
            if (webhookRepository.existsByWebhookUuid(webhookUuid)) {
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

    // 웹훅 원본 문자열을 JsonNode로 파싱한다
    private JsonNode readJson(String rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception e) {
            throw new PortOneException(ErrorCode.INVALID_WEBHOOK_JSON);
        }
    }

    // 웹훅 바디에서 status 값을 추출한다
    private String readEventStatus(JsonNode root) {
        String nestedStatus = root.path("data").path("status").asText(null);
        if (nestedStatus != null && !nestedStatus.isBlank()) {
            return nestedStatus;
        }

        String rootStatus = root.path("status").asText(null);
        if (rootStatus != null && !rootStatus.isBlank()) {
            return rootStatus;
        }
        return null;
    }
}