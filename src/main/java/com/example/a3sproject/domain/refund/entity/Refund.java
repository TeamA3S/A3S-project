package com.example.a3sproject.domain.refund.entity;

import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.refund.enums.RefundStatus;
import com.example.a3sproject.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "refunds",
        uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_refunds_payment_id",
                columnNames = {"payment_id"}
        )
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(nullable = false)
    private int refundAmount;

    @Column(nullable = false)
    private String refundReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus refundStatus;

    private OffsetDateTime refundedAt;

    public Refund(Payment payment, String refundReason) {
        this.payment = payment;
        this.refundAmount = payment.getPaidAmount();
        this.refundReason = refundReason;
        this.refundStatus = RefundStatus.REQUEST;
    }

    // 환불 완료 상태 변경
    public void completeRefund(OffsetDateTime refundedAt) {
        this.refundStatus = RefundStatus.COMPLETED;
        this.refundedAt = refundedAt;
    }

    // 환불 실패 상태 변경
    public void cancelRefund() {
        this.refundStatus = RefundStatus.FAILED;
    }
}
