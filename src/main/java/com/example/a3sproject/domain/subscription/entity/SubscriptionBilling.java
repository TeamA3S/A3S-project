package com.example.a3sproject.domain.subscription.entity;

import com.example.a3sproject.domain.subscription.enums.SubscriptionBillingStatus;
import com.example.a3sproject.global.common.GenerateCodeUuid;
import com.example.a3sproject.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "subscription_billings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_subscription_billings_subscription_id",
                        columnNames = {"subscription_id", "periodStart"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionBilling extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_billing_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    //    private String subscriptionUuid; // 구독 아이디
    private int amount; // 청구 금액

    @Enumerated(EnumType.STRING)
    private SubscriptionBillingStatus status; // 청구 상태
    private String paymentId; // 결제 아이디
    //    private OffsetDateTime attemptDate; //결제시도일 //createdAt
    private OffsetDateTime periodStart; //청구기간 시작일
    private OffsetDateTime periodEnd; //청구기간 종료일
    private String failureMessage; // 실패 메세지

    @Column(unique = true)
    private String billingHistoryUuid;

    public SubscriptionBilling(Subscription subscription, int amount, String paymentId) {
        this.subscription = subscription;
        this.amount = amount;
        this.paymentId = paymentId;
        this.status = SubscriptionBillingStatus.PENDING;
        this.billingHistoryUuid = GenerateCodeUuid.generateCodeUuid("BIH");
    }

    public void AutoSubscriptionBillingSuccess() {
        this.status = SubscriptionBillingStatus.PAID;
        this.periodStart = OffsetDateTime.now();
        this.periodEnd = OffsetDateTime.now().plusMonths(1);
    }

    public void AutoSubscriptionBillingFailure(String failureMessage) {
        this.status = SubscriptionBillingStatus.FAILED;
        this.failureMessage = failureMessage;
    }

    public SubscriptionBilling(Subscription subscription, int amount, SubscriptionBillingStatus status, String paymentId,
                               OffsetDateTime periodStart, OffsetDateTime periodEnd, String failureMessage) {
        this.subscription = subscription;
        this.amount = amount;
        this.status = status;
        this.paymentId = paymentId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.failureMessage = failureMessage;
        this.billingHistoryUuid = GenerateCodeUuid.generateCodeUuid("BIH");
    }
}
