package com.example.a3sproject.domain.order.controller;

import com.example.a3sproject.domain.order.dto.CreateOrderRequestDto;
import com.example.a3sproject.domain.order.dto.CreateOrderResponseDto;
import com.example.a3sproject.domain.order.service.OrderService;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 주문 생성
    @PostMapping("/orders")
    public ResponseEntity<ApiResponseDto<CreateOrderResponseDto>> createOrder(
            @AuthenticationPrincipal User user,
            @RequestBody CreateOrderRequestDto requestDto
    ) {
        CreateOrderResponseDto response = orderService.createOrder(user, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(HttpStatus.CREATED, response));
    }
}
