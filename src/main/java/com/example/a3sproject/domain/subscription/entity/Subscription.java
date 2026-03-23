package com.example.a3sproject.domain.subscription.entity;

import com.example.a3sproject.domain.plan.entity.Plan;
import com.example.a3sproject.domain.paymentMethod.entity.PaymentMethod;
import com.example.a3sproject.domain.subscription.enums.SubscriptionStatus;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.global.common.GenerateCodeUuid;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status; //구독 상태
    private OffsetDateTime currentPeriodEnd; // 현재 이용기간 종료일
    private OffsetDateTime canceledAt;
    private int amount;


    public Subscription(User user, Plan plan, PaymentMethod paymentMethod, int amount) {
        this.subscriptionUuid = GenerateCodeUuid.generateCodeUuid("SUBS");
        this.user = user;
        this.plan = plan;
        this.paymentMethod = paymentMethod;
        this.status = SubscriptionStatus.ACTIVE;
        this.currentPeriodEnd = OffsetDateTime.now().plusMonths(1); //한달후
        this.amount = amount;
    }

    // 구독 기간 갱신 (정기권 결제 성공시)
    public void renewPeriod() {
        this.currentPeriodEnd = this.currentPeriodEnd.plusMonths(1);
        this.status = SubscriptionStatus.ACTIVE;
    }

    // 구독 상태 변경 - 연체(미납) (정기권 결제 실패시)
    public void markAsPastDue() {
        this.status = SubscriptionStatus.PAST_DUE;
    }
}
