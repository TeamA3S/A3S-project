package com.example.a3sproject.domain.point.entity;

import com.example.a3sproject.domain.point.enums.PointTransactionType;
import com.example.a3sproject.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "points")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Point extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자 ID (User와 직접 연관관계 대신 ID만 보관)
    @Column(nullable = false)
    private Long userId;

    // 주문 ID (주문과 무관한 거래는 null 허용)
    @Column
    private Long orderId;

    // 포인트 변동액 (양수: 적립/복구, 음수: 사용/소멸)
    @Column(nullable = false)
    private int points;

    // 거래 타입
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointTransactionType type;

    // 만료일 (소멸 정책 적용 시)
    @Column
    private LocalDateTime expiredAt;

    // 정적 팩토리 메서드
    public static Point of(Long userId, Long orderId,
                           int points, PointTransactionType type,
                           LocalDateTime expiredAt) {
        Point tx = new Point();
        tx.userId = userId;
        tx.orderId = orderId;
        tx.points = points;
        tx.type = type;
        tx.expiredAt = expiredAt;
        return tx;
    }
}
