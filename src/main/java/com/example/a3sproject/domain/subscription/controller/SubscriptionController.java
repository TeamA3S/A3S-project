package com.example.a3sproject.domain.subscription.controller;


import com.example.a3sproject.domain.subscription.dtos.request.CreateSubscriptionRequest;
import com.example.a3sproject.domain.subscription.dtos.response.CreateSubscriptionReaponse;
import com.example.a3sproject.domain.subscription.dtos.response.GetSubscriptionReaponse;
import com.example.a3sproject.domain.subscription.dtos.response.UpdateSubscriptionReaponse;
import com.example.a3sproject.global.dto.ApiResponseDto;
import com.example.a3sproject.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.tools.Trace;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscription")
public class SubscriptionController {

    // create-subscription
    @PostMapping
    public ResponseEntity<ApiResponseDto<CreateSubscriptionReaponse>> CreateSubscription(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody CreateSubscriptionRequest request
    ) { }

    // get-subscription
    @GetMapping("/{subscriptionId}")
    public ResponseEntity<ApiResponseDto<GetSubscriptionReaponse>> GetSubscription( //Todo : Trace trace가 자동완성으로 들어왔는데 뭐였는지 확인할것
                                                                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                                                                    @PathVariable String subscriptionId
    ) {
    }
    // update-subscription
    @PutMapping("/{subscriptionId}")
    public ResponseEntity<ApiResponseDto<UpdateSubscriptionReaponse>> UpdateSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String subscriptionId
    ){

    }
}