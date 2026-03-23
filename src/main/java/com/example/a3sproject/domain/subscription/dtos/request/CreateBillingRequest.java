package com.example.a3sproject.domain.subscription.dtos.request;

import lombok.Getter;

@Getter
public class CreateBillingRequest {
    private String periodStart;
    private String periodEnd;
}
