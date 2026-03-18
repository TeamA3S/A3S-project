package com.example.a3sproject.domain.point.service;

import com.example.a3sproject.domain.point.dto.MyPointTransactionResponseDto;
import com.example.a3sproject.domain.point.entity.PointTransaction;
import com.example.a3sproject.domain.point.enums.PointTransactionType;
import com.example.a3sproject.domain.point.repository.PointRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointRepository pointRepository;
    private final UserRepository userRepository;

    // 포인트 검증 및 차감
    @Transactional
    public void validateAndUse(Long userId, int amount) {

        // 유저 조회
        User user = userRepository.findWithLockById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 포인트 잔액 차감 (잔액 부족 시 PointException 자동 발생)
        user.usePoint(amount);

        // 포인트 거래 이력 기록
        PointTransaction tx = PointTransaction.of(
                userId,
                null,              // 주문 ID는 결제 완료 후 연결
                -amount,                  // 차감이므로 음수
                user.getPointBalance(),   // 차감 후 잔액 스냅샷
                PointTransactionType.USE, // 만료일 없음
                null
        );
        pointRepository.save(tx);
    }

    // 포인트 적립
    @Transactional
    public void earnPoint(Long userId, Long orderId, int amount) {

        // 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 포인트 잔액 적립
        user.earnPoint(amount);

        // 포인트 거래 이력 기록
        PointTransaction tx = PointTransaction.of(
                userId,
                orderId,
                amount,
                user.getPointBalance(),
                PointTransactionType.EARN,
                null
        );
        pointRepository.save(tx);
    }

    // 포인트 복수 (결제 실패 시 자동 호출)
    @Transactional
    public void restorePoint(Long userId, Long orderId, int amount) {

        // 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 포인트 잔액 복구
        user.restorePoint(amount);

        // 포인트 거래 이력 기록
        PointTransaction tx = PointTransaction.of(
                userId,
                orderId,                      // 연관된 주문 ID
                amount,                       // 복구이므로 양수
                user.getPointBalance(),       // 복구 후 잔액 스냅샷
                PointTransactionType.RESTORE,
                null                          // 만료일 없음
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
}