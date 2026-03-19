package com.example.a3sproject.domain.payment.controller;

import com.example.a3sproject.domain.payment.dto.request.PaymentConfirmRequest;
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

    @PostMapping("/confirm") // 결제 확정 API - Todo : 일단은 해피패스만 완료
    public ResponseEntity<ApiResponseDto<PaymentConfirmResponse>> confirmPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails, // Todo : 소유권 검증
            @RequestBody PaymentConfirmRequest request
    ) {
        PaymentConfirmResponse response = paymentService.confirmPayment(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, response));
    }

    // 웹훅수신
}
