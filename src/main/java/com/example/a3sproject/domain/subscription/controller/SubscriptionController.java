package com.example.a3sproject.domain.subscription.controller;


import com.example.a3sproject.domain.subscription.dtos.request.CreateBillingRequest;
import com.example.a3sproject.domain.subscription.dtos.request.CreateSubscriptionRequest;
import com.example.a3sproject.domain.subscription.dtos.request.UpdateSubscriptionRequest;
import com.example.a3sproject.domain.subscription.dtos.response.CreateBillingResponse;
import com.example.a3sproject.domain.subscription.dtos.response.CreateSubscriptionResponse;
import com.example.a3sproject.domain.subscription.dtos.response.GetAllBillingsResponse;
import com.example.a3sproject.domain.subscription.dtos.response.GetSubscriptionResponse;
import com.example.a3sproject.domain.subscription.service.SubscriptionService;
import com.example.a3sproject.global.dto.ApiResponseDto;
import com.example.a3sproject.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    // create-subscription
    @PostMapping
    public ResponseEntity<ApiResponseDto<CreateSubscriptionResponse>> CreateSubscription(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody CreateSubscriptionRequest request
    ) {
        CreateSubscriptionResponse response = subscriptionService.createSubscription(customUserDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.success(HttpStatus.CREATED, response));
    }

    // get-subscription
    @GetMapping("/{subscriptionId}")
    public ResponseEntity<ApiResponseDto<GetSubscriptionResponse>> GetSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String subscriptionId
    ) {
        GetSubscriptionResponse response = subscriptionService.getSubscription(userDetails.getUser(), subscriptionId);
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, response));
    }

    @GetMapping("/{subscriptionId}/billings")
    public ResponseEntity<ApiResponseDto<GetAllBillingsResponse>> GetBillings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String subscriptionId
    ){
        GetAllBillingsResponse response = subscriptionService.getBillings(userDetails.getUser(), subscriptionId);
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK,  response));

    }

    // 수동 즉시 청구 (빌링키로 결제)
    @PostMapping("/{subscriptionId}/billings")
    public ResponseEntity<ApiResponseDto<CreateBillingResponse>> createBilling(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String subscriptionId,
            @RequestBody CreateBillingRequest request
    ) {
        CreateBillingResponse response = subscriptionService.createBilling(userDetails.getId(), subscriptionId, request);
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, response));
    }

    // 구독 해지
    @PatchMapping("/{subscriptionId}")
    public ResponseEntity<ApiResponseDto<Void>> cancelSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String subscriptionId,
            @RequestBody UpdateSubscriptionRequest request
    ) {
        if ("cancel".equals(request.action())) {
            subscriptionService.cancelSubscription(userDetails.getId(),subscriptionId);
        }
        return ResponseEntity.ok(ApiResponseDto.successWithNoContent());
    }
}