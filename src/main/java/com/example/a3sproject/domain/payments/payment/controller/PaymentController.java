package com.example.a3sproject.domain.payments.payment.controller;


import com.example.a3sproject.domain.payments.payment.dto.request.PaymentConfirmRequest;
import com.example.a3sproject.domain.payments.payment.dto.response.PaymentConfirmResponse;
import com.example.a3sproject.domain.payments.payment.service.PaymentService;
import com.example.a3sproject.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;




@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;

    // 결제 시도 기록 API

    @PostMapping("/confirm") // 결제 확정 API - Todo : 일단은 해피패스만 완료
    public ResponseEntity<ApiResponseDto<PaymentConfirmResponse>> confirmPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PaymentConfirmRequest request
    ) {
        PaymentConfirmResponse response = paymentService.confirmPayment(request);
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, response));
    }

    // 웹훅수신


}
