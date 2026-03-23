package com.example.a3sproject.domain.subscription.controller;


import com.example.a3sproject.domain.subscription.dtos.request.CreateSubscriptionRequest;
import com.example.a3sproject.domain.subscription.dtos.request.UpdateSubscriptionRequest;
import com.example.a3sproject.domain.subscription.dtos.response.CreateSubscriptionResponse;
import com.example.a3sproject.domain.subscription.dtos.response.GetSubscriptionResponse;
import com.example.a3sproject.domain.subscription.dtos.response.UpdateSubscriptionResponse;
import com.example.a3sproject.global.dto.ApiResponseDto;
import com.example.a3sproject.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    // create-subscription
    @PostMapping
    public ResponseEntity<ApiResponseDto<CreateSubscriptionResponse>> CreateSubscription(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody CreateSubscriptionRequest request
    ) {
    }

    // get-subscription
    @GetMapping("/{subscriptionId}")
    public ResponseEntity<ApiResponseDto<GetSubscriptionResponse>> GetSubscription( //Todo : Trace trace가 자동완성으로 들어왔는데 뭐였는지 확인할것
                                                                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                                                                    @PathVariable String subscriptionId
    ) {
    }

    // update-subscription
    @PatchMapping ("/{subscriptionId}")
    public ResponseEntity<ApiResponseDto<UpdateSubscriptionResponse>> UpdateSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String subscriptionId,
            @RequestBody UpdateSubscriptionRequest request
    ) {

    }

    @GetMapping("/{subscriptionId}/billings")
    public ResponseEntity<ApiResponseDto<GetSubscriptionResponse>> GetBillings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String subscriptionId
    ){

    }
}