package com.example.a3sproject.domain.subscription.entity;

import com.example.a3sproject.domain.subscription.enums.SubscriptionBillingStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
public class SubscriptionBilling {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_billing_id")
    private Long id;

    private String subscriptionId; // 구독 아이디
    private int amount; // 청구 금액
    private SubscriptionBillingStatus status; // 청구 상태
    private String paymentId; // 결제 아이디
    private OffsetDateTime attemptDate; //결제시도일
    private OffsetDateTime periodStart; //청구기간 시작일
    private OffsetDateTime periodEnd; //청구기간 종료일
    private String failureMessage; // 실패 메세지
}
