package com.example.a3sproject.domain.plan.dto;

import lombok.Getter;

@Getter
public class GetPlanResponseDto {

    private final String planId; // TODO: 아마 uuid일듯.
    private final String name;
    private final int amount;
    private final String billingCycle;

    public GetPlanResponseDto(String planId, String name, int amount, String billingCycle) {
        this.planId = planId;
        this.name = name;
        this.amount = amount;
        this.billingCycle = billingCycle;
    }
}
