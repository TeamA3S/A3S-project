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

    private String portOneId;
    private int paidAmount;
    @Enumerated(EnumType.STRING)
    private PaidStatus paidStatus;
    // 결제일
    // 환불일
    private OffsetDateTime  paidAt; // 결제완료 시각

    private String paymentUuid;

    public Payment(Order order, int paidAmount, String paymentUuid) {
        this.order = order;
        this.paidAmount = paidAmount;
        this.paidStatus = PaidStatus.PENDING;
        this.paymentUuid = paymentUuid;
    }

    // 결제 확정 시점 → 세 값이 항상 함께 변경됨
    public void confirmPayment(String portOneId, OffsetDateTime paidAt) {
        this.portOneId = portOneId;
        this.paidAt = paidAt;
        this.paidStatus = PaidStatus.SUCCESS;
    }

    // 결제 실패 시점 → 상태만 변경
    public void failPayment() {
        this.paidStatus = PaidStatus.FAILED;
    }

    // 환불 완료 시점 → 상태만 변경
    public void refundPayment() {
        this.paidStatus = PaidStatus.REFUNDED;
    }
}