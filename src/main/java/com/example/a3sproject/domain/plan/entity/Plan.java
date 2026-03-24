package com.example.a3sproject.domain.plan.entity;

import com.example.a3sproject.global.common.GenerateCodeUuid;
import com.example.a3sproject.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Table(name = "plans")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private int amount;
    private String billingCycle; // 결제 주기
    private boolean active;
    @Column(unique = true)
    private String planUuid;

    public Plan(String name, int amount, String billingCycle, boolean active) {
        this.name = name;
        this.amount = amount;
        this.billingCycle = billingCycle;
        this.active = active;
        this.planUuid = GenerateCodeUuid.generateCodeUuid("PLAN");
    }
}
