package com.example.a3sproject.domain.payment.controller;

import com.example.a3sproject.domain.payment.dto.request.PaymentTryRequest;
import com.example.a3sproject.domain.payment.dto.response.PaymentConfirmResponse;
import com.example.a3sproject.domain.payment.dto.response.PaymentTryResponse;
import com.example.a3sproject.domain.payment.service.PaymentService;
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
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/attempt")// 결제 시도 기록 API
    public ResponseEntity<ApiResponseDto<PaymentTryResponse>> createPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PaymentTryRequest request
    ) {
        PaymentTryResponse response = paymentService.createPayment(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(HttpStatus.CREATED, response));
    }

    @PostMapping("/{portOneId}/confirm") // 결제 확정 API
    public ResponseEntity<ApiResponseDto<PaymentConfirmResponse>> confirmPayment(
            @PathVariable String portOneId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PaymentConfirmResponse response = paymentService.confirmPayment(portOneId, userDetails.getId());
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, response));
    }

}
