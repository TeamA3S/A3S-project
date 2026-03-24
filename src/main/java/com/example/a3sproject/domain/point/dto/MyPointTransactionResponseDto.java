package com.example.a3sproject.domain.point.dto;

import com.example.a3sproject.domain.point.entity.PointTransaction;
import com.example.a3sproject.domain.point.enums.PointTransactionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MyPointTransactionResponseDto {
    private final int points;                    // 변동액
    private final PointTransactionType type;     // 거래 타입
    private final LocalDateTime createdAt;       // 날짜
    private final int balance;                   // 포인트 잔액 (스냅샷)

    public static MyPointTransactionResponseDto from(PointTransaction pointTransaction) {
        return MyPointTransactionResponseDto.builder()
                .points(pointTransaction.getPoints())
                .type(pointTransaction.getType())
                .createdAt(pointTransaction.getCreatedAt())
                .balance(pointTransaction.getPointBalance())
                .build();
    }
}