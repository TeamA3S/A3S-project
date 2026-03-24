package com.example.a3sproject.domain.subscription.dtos.request;


import java.time.OffsetDateTime;

public record CreateBillingRequest (
    OffsetDateTime periodStart,
    OffsetDateTime periodEnd
){}
