package com.example.a3sproject.domain.point.entity;

import com.example.a3sproject.domain.point.enums.PointTransactionType;
import com.example.a3sproject.global.entity.BaseEntity;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.PointException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "points")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointTransaction extends BaseEntity {

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

    // 포인트 잔액 (스냅샷)
    @Column(nullable = false)
    private int pointBalance;

    // 거래 타입
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointTransactionType type;

    // 만료일 (소멸 정책 적용 시)
    @Column
    private LocalDateTime expiredAt;

    // 정적 팩토리 메서드
    public static PointTransaction of(Long userId, Long orderId,
                                      int points, PointTransactionType type,
                                      LocalDateTime expiredAt) {
        PointTransaction tx = new PointTransaction();
        tx.userId = userId;
        tx.orderId = orderId;
        tx.points = points;
        tx.type = type;
        tx.expiredAt = expiredAt;
        return tx;
    }

    // 포인트 차감
    public void usePoint(int amount) {
        if (this.pointBalance < amount) {
            throw new PointException(ErrorCode.POINT_NOT_ENOUGH);
        }
        this.pointBalance -= amount;
    }

    // 포인트 적립
    public void earnPoint(int amount) {
        this.pointBalance += amount;
    }

    // 포인트 복구
    public void restorePoint(int amount) {
        this.pointBalance += amount;
    }
}
