package com.example.a3sproject.domain.plan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class GetPlanResponseDto {

    @JsonProperty("planId")
    private final String planUuid; // TODO: 아마 uuid일듯.
    private final String name;
    private final int amount;
    private final String billingCycle;

    public GetPlanResponseDto(String planUuid, String name, int amount, String billingCycle) {
        this.planUuid = planUuid;
        this.name = name;
        this.amount = amount;
        this.billingCycle = billingCycle;
    }
}
