package com.example.a3sproject.domain.subscription.entity;

import com.example.a3sproject.domain.plan.entity.Plan;
import com.example.a3sproject.domain.subscription.enums.SubscriptionStatus;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.global.common.GenerateCodeUuid;
import com.example.a3sproject.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "subscription") // 검토 필요
    private List<SubscriptionBilling> subscriptionBilling = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    private long paymentMethodId; // 결제수단 아이디

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status; //구독 상태

    private OffsetDateTime currentPeriodEnd; // 현재 이용기간 종료일

    private OffsetDateTime canceledAt;

    private int amount;


    public Subscription(User user, Plan plan, long paymentMethodId, int amount) {
        this.subscriptionUuid = GenerateCodeUuid.generateCodeUuid("SUBS");
        this.user = user;
        this.plan = plan;
        this.paymentMethodId = paymentMethodId;
        this.status = SubscriptionStatus.ACTIVE;
        this.currentPeriodEnd = OffsetDateTime.now().plusMonths(1); //한달후
        this.amount = amount;
    }
}
