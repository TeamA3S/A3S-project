package com.example.a3sproject.domain.subscription.dto.response;



import java.util.List;

public record GetAllBillingsResponse(
        List<GetBillingResponse> billings
) {
//    public record GetbillingResponse(
//            String billingId,
//            String periodStart,
//            String periodEnd,
//            int amount,
//            String status,
//            @Nullable String paymentId, //(선택)
//            @Nullable String attemptDate, //(선택)
//            @Nullable String failureMessage //(선택)
//    ) { }
}