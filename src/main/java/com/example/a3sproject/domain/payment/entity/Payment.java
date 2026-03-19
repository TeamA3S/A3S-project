package com.example.a3sproject.domain.payment.entity;

import com.example.a3sproject.domain.order.entity.Order;
import com.example.a3sproject.domain.payment.enums.PaidStatus;
import com.example.a3sproject.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "payments",
        uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_payments_order_id",
                columnNames = {"order_id"}
        )
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "portOne_id", nullable = false, unique = true, updatable = false)
    private String portOneId;

    @Column(nullable = false)
    private int paidAmount;
    private int pointsToUse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaidStatus paidStatus;

    // 결제일
    @Column
    private OffsetDateTime paidAt;
    // 환불일
    @Column
    private OffsetDateTime  refundedAt;


    public Payment(Order order, int paidAmount, String portOneId, int pointsToUse) {
        this.order = order;
        this.paidAmount = paidAmount;
        this.paidStatus = PaidStatus.PENDING;
        this.portOneId = portOneId;
        this.pointsToUse = pointsToUse;
    }

    // 기존 결제를 다시 결제 시도 가능한 상태로 덮어쓴다 (기록 재활용)
    public void preparePendingAttempt(int paidAmount) {
        this.paidAmount = paidAmount;
        this.paidStatus = PaidStatus.PENDING;
        this.paidAt = null;
    } // portOneId null 아니어도 됩니당

    // 이미 끝난 결제인지 확인
    public boolean isFinalized() {
        return this.paidStatus == PaidStatus.SUCCESS || this.paidStatus == PaidStatus.REFUNDED;
    }

    // 결제 확정 시점 → 세 값이 항상 함께 변경됨
    public void confirmPayment(OffsetDateTime paidAt) {
        this.paidAt = paidAt;
        this.paidStatus = PaidStatus.SUCCESS;
    }

    // 결제 실패 시점 → 상태만 변경
    public void failStatus() {
        this.paidStatus = PaidStatus.FAILED;
    }

    // 환불 완료 시점 → 상태만 변경
    public void refundStatus(OffsetDateTime refundedAt) {
        this.paidStatus = PaidStatus.REFUNDED;
        this.refundedAt = refundedAt;
    }
}