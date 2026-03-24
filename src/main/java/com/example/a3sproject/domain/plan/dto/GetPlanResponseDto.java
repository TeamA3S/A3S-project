package com.example.a3sproject.domain.plan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class GetPlanResponseDto {

//    @JsonProperty("planId")
    private final String planId;
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
