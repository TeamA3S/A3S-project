package com.example.a3sproject.domain.point.controller;

import com.example.a3sproject.domain.point.dto.MyPointTransactionResponseDto;
import com.example.a3sproject.domain.point.service.PointService;
import com.example.a3sproject.global.dto.ApiResponseDto;
import com.example.a3sproject.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    // 내 포인트 거래 내역 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponseDto<List<MyPointTransactionResponseDto>>> getMyPointTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        List<MyPointTransactionResponseDto> response = pointService.getMyPointTransactions(userDetails.getEmail());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponseDto.success(HttpStatus.OK, response));
    }
}
