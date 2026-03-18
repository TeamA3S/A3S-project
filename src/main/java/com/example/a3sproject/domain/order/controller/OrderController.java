package com.example.a3sproject.domain.order.controller;

import com.example.a3sproject.domain.order.dto.CreateOrderRequestDto;
import com.example.a3sproject.domain.order.dto.CreateOrderResponseDto;
import com.example.a3sproject.domain.order.dto.GetOrderListResponseDto;
import com.example.a3sproject.domain.order.service.OrderService;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.global.dto.ApiResponseDto;
import com.example.a3sproject.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 주문 생성
    @PostMapping("/orders")
    public ResponseEntity<ApiResponseDto<CreateOrderResponseDto>> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CreateOrderRequestDto requestDto
    ) {
        CreateOrderResponseDto response = orderService.createOrder(userDetails.getId(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(HttpStatus.CREATED, response));
    }

    // 주문 목록 조회
    @GetMapping("/orders")
    public ResponseEntity<ApiResponseDto<List<GetOrderListResponseDto>>> getAllOrderList(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, orderService.getAllOrderList(userDetails.getId())));
    }
}
