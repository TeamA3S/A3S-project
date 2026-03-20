package com.example.a3sproject.domain.paymentMethod.entity;

import com.example.a3sproject.domain.paymentMethod.enums.PaymentMethodStatus;
import com.example.a3sproject.domain.paymentMethod.enums.PgProvider;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Table(name = "paymentMethods")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentMethod extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String billingKey; // PortOne이 발급해주는 키

    private String customerUid;

    @Enumerated(EnumType.STRING)
    private PgProvider pgProvider;

    private boolean isDefault;

    @Enumerated(EnumType.STRING)
    private PaymentMethodStatus status;

    public PaymentMethod(User user, String billingKey, String customerUid,
                         PgProvider pgProvider, boolean isDefault, PaymentMethodStatus status) {
        this.user = user;
        this.billingKey = billingKey;
        this.customerUid = customerUid;
        this.pgProvider = pgProvider;
        this.isDefault = isDefault;
        this.status = status;
    }
}
