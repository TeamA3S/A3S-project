package com.example.a3sproject.domain.portone.webhook.entity;

import com.example.a3sproject.domain.portone.webhook.enums.WebhookStatus;
import com.example.a3sproject.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "webhook_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_webhook_events_webhook_id",
                        columnNames = {"webhook_id"}
                )
        }
)
public class Webhook extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "webhook_id")
    private String webhookUuid;
    private String portOneId;
    private String eventStatus;
    @Enumerated(EnumType.STRING)
    private WebhookStatus status;

    public Webhook(String webhookUuid, String portOneId, String eventStatus) {
        this.webhookUuid = webhookUuid;
        this.portOneId = portOneId;
        this.eventStatus = eventStatus;
        this.status = WebhookStatus.RECEIVED;
    }

    public void processedWebhook() {
        this.status = WebhookStatus.PROCESSED;
    }

    public void failedWebhook() {
        this.status = WebhookStatus.FAILED;
    }
}
