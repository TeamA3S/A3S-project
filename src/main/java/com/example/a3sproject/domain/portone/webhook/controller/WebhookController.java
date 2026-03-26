package com.example.a3sproject.domain.portone.webhook.controller;

import com.example.a3sproject.domain.portone.webhook.dto.request.WebhookRequest;
import com.example.a3sproject.domain.portone.webhook.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/webhook")
public class WebhookController {
    private final WebhookService webhookService;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String rawBody
    ) {
        System.out.println("checkcheckcheckcheckcheckcheck 9999");
        try {
            log.info("rawBody: {}", rawBody);
            System.out.println("checkcheckcheckcheckcheckcheck 10101010");
            webhookService.handleWebhook(rawBody);
        } catch (Exception e) {
            log.error("웹훅 처리 실패 rawBody: {}, error: {}", rawBody, e.getMessage());
            // 실패해도 200 OK 반환! (PortOne 재전송 방지)
        }
        return ResponseEntity.ok().build();
    }
}