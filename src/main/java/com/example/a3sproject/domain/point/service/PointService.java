package com.example.a3sproject.domain.point.service;

import com.example.a3sproject.domain.point.dto.MyPointTransactionResponseDto;
import com.example.a3sproject.domain.point.entity.PointTransaction;
import com.example.a3sproject.domain.point.enums.PointTransactionType;
import com.example.a3sproject.domain.point.repository.PointRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.PointException;
import com.example.a3sproject.global.exception.domain.RefundException;
import com.example.a3sproject.global.exception.domain.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointRepository pointRepository;
    private final UserRepository userRepository;

    // 포인트 검증 및 차감 (FIFO 방식)
    @Transactional
    public void validateAndUse(Long userId, Long orderId, int amount) {

        // 1. 유저 조회 (비관적 락)
        User user = userRepository.findWithLockById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 잔액 검증
        if (user.getPointBalance() < amount) {
            throw new PointException(ErrorCode.POINT_NOT_ENOUGH);
        }

        // 3. FIFO 방식으로 만료일 빠른 순서로 조회
        List<PointTransaction> earnedTransactions = pointRepository
                .findByUserIdAndTypeAndRemainingPointsGreaterThanOrderByExpiredAtAsc(
                        userId,
                        PointTransactionType.EARN,
                        0 // remainingPoints > 0 인 것만
                );

        // 4. 순서대로 차감
        int remainingAmount = amount;
        for (PointTransaction tx : earnedTransactions) {
            if (remainingAmount <= 0) break;
            int deducted = tx.deductRemaining(remainingAmount);
            remainingAmount -= deducted;
        }

        // 5. User 잔액 차감 (잔액 부족 시 PointException 자동 발생)
        user.usePoint(amount);

        // 6. USE 타입 포인트 거래 이력 기록
        PointTransaction tx = PointTransaction.of(
                userId,
                orderId,                  // 주문 ID는 결제 완료 후 연결
                -amount,                  // 차감이므로 음수
                user.getPointBalance(),   // 차감 후 잔액 스냅샷
                PointTransactionType.USE,
                0,           // USE 타입은 remainingPoints = 0
                null                      // USE 타입은 만료일 없음
        );
        pointRepository.save(tx);
    }

    // 포인트 적립
    @Transactional
    public void earnPoint(Long userId, Long orderId, int amount) {

        // 1. 유저 조회
        User user = userRepository.findWithLockById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 포인트 잔액 적립
        user.earnPoint(amount);

        // 3. 포인트 거래 이력 기록
        PointTransaction tx = PointTransaction.of(
                userId,
                orderId,
                amount,                          // 변동액 (양수)
                user.getPointBalance(),          // 적립 후 잔액 스냅샷
                PointTransactionType.EARN,
                amount,                          // remainingPoints = 적립액 전체
                LocalDateTime.now().plusYears(1) // 만료일 = 적립일로부터 1년
        );
        pointRepository.save(tx);
    }

    // 포인트 복구 (결제 실패 시 자동 호출)
    @Transactional
    public void restorePoint(Long userId, Long orderId, int amount) {

        // 1. 유저 조회 (비관적 락)
        User user = userRepository.findWithLockById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 포인트 잔액 복구
        user.restorePoint(amount);

        // 3. RESTORE 타입 포인트 거래 이력 기록
        PointTransaction tx = PointTransaction.of(
                userId,
                orderId,                      // 연관된 주문 ID
                amount,                       // 복구이므로 양수
                user.getPointBalance(),       // 복구 후 잔액 스냅샷
                PointTransactionType.RESTORE,
                0,               //RESTORE 타입은 remainingPoints = 0
                null                          //RESTORE 타입은 만료일 없음
        );
        pointRepository.save(tx);
    }

    // 내 포인트 거래 내역 조회
    public List<MyPointTransactionResponseDto> getMyPointTransactions(String email) {

        // 1. 이메일로 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. userId로 포인트 거래 내역 조회 (최신순)
        List<PointTransaction> transactions = pointRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId());

        // 3. DTO 변환 후 반환
        return transactions.stream()
                .map(MyPointTransactionResponseDto::from)
                .toList();
    }

    // 적립 포인트 취소 (환불 시 호출)
    @Transactional
    public void cancelEarnedPoint(Long userId, Long orderId, int amount) {

        // 유저 조회
        User user = userRepository.findWithLockById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 적립된 포인트 차감
        user.usePoint(amount);

        // 포인트 거래 이력 기록
        PointTransaction tx = PointTransaction.of(
                userId,
                orderId,
                -amount,                     // 차감이므로 음수
                user.getPointBalance(),      // 차감 후 잔액 스냅샷
                PointTransactionType.CANCEL,
                0,              // CANCEL 타입은 remainingPoints = 0
                null                         // CANCEL 타입은 만료일 없음
        );
        pointRepository.save(tx);
    }
}