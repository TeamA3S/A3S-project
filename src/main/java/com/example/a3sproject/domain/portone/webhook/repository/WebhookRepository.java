package com.example.a3sproject.domain.portone.webhook.repository;


import com.example.a3sproject.domain.portone.webhook.entity.Webhook;
import com.example.a3sproject.domain.portone.webhook.enums.WebhookStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookRepository extends JpaRepository<Webhook, Long> {
    boolean existsByWebhookUuidAndStatus(String webhookUuid, WebhookStatus Status);

    boolean existsByWebhookUuid(String webhookUuid);
}
