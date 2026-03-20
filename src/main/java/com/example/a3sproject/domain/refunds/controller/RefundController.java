package com.example.a3sproject.domain.refunds.controller;

import com.example.a3sproject.domain.refunds.dto.request.RefundRequestDto;
import com.example.a3sproject.domain.refunds.dto.response.RefundResponseDto;
import com.example.a3sproject.domain.refunds.service.RefundService;
import com.example.a3sproject.global.dto.ApiResponseDto;
import com.example.a3sproject.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/refunds")
public class RefundController {
    private final RefundService refundService;

    // 환불
    @PostMapping("/{portOneId}")
    public ResponseEntity<ApiResponseDto<RefundResponseDto>> refundPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String portOneId,
            @RequestBody RefundRequestDto requestDto
    ) {
        RefundResponseDto response = refundService.refundPayment(userDetails.getId(), portOneId, requestDto);
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.CREATED, response));
    }
}
