package com.example.a3sproject.domain.subscription.entity;

import com.example.a3sproject.domain.subscription.enums.SubscriptionStatus;
import com.example.a3sproject.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "subscriptions" /**,
 *   uniqueConstraints = {
 *           @UniqueConstraint(
 *                   name = "uk_payments_order_id",
 *                   columnNames = {"order_id"}
 *           )
}*/)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long id;

    @Column(unique = true)
    private String subscriptionUuid;
    private String customerUid; // 고객 아이디
    private String planId; // 플랜 아이디
    private long paymentMethodId; // 결제수단 아이디
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status; //구독 상태
    private OffsetDateTime currentPeriodEnd; // 현재 이용기간 종료일
    private OffsetDateTime canceledAt;
    private int amount;

}
