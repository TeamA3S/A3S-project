package com.example.a3sproject.domain.subscription.entity;

import com.example.a3sproject.domain.subscription.enums.SubscriptionBillingStatus;
import com.example.a3sproject.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "subscription_billings" /**,
     *   uniqueConstraints = {
     *           @UniqueConstraint(
     *                   name = "uk_payments_order_id",
     *                   columnNames = {"order_id"}
     *           )
        }*/)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionBilling extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_billing_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    private String subscriptionUuid; // 구독 아이디
    private int amount; // 청구 금액
    @Enumerated(EnumType.STRING)
    private SubscriptionBillingStatus status; // 청구 상태
    private String paymentId; // 결제 아이디
    private LocalDateTime attemptDate; //결제시도일
    private LocalDateTime periodStart; //청구기간 시작일
    private LocalDateTime periodEnd; //청구기간 종료일
    private String failureMessage; // 실패 메세지

    public SubscriptionBilling(Subscription subscription, int amount, SubscriptionBillingStatus status, String paymentId,
                               LocalDateTime attemptDate, LocalDateTime periodStart, LocalDateTime periodEnd, String failureMessage) {
        this.subscription = subscription;
        this.amount = amount;
        this.status = status;
        this.paymentId = paymentId;
        this.attemptDate = attemptDate;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.failureMessage = failureMessage;
    }
}
