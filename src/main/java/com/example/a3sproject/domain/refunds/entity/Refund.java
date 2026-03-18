package com.example.a3sproject.domain.refunds.entity;

import com.example.a3sproject.domain.payment.entity.Payment;
import com.example.a3sproject.domain.refunds.enums.RefundStatus;
import com.example.a3sproject.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    private Long refundAmount;
    private String refundReason;
    private RefundStatus refundStatus;
    // 환불 처리 시각


}
