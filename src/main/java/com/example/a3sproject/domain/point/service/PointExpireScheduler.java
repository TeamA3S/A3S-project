package com.example.a3sproject.domain.point.service;

import com.example.a3sproject.domain.point.entity.PointTransaction;
import com.example.a3sproject.domain.point.enums.PointTransactionType;
import com.example.a3sproject.domain.point.repository.PointRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointExpireScheduler {

    private final PointRepository pointRepository;
    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void expirePoints() {
        log.info("포인트 소멸 스케줄러 시작");

        // 1. 만료일 도래한 EARN 타입 거래 조회
        List<PointTransaction> expiredTransactions = pointRepository
                .findByTypeAndExpiredAtBeforeAndRemainingPointsGreaterThan(
                        PointTransactionType.EARN,
                        LocalDateTime.now(),
                        0
                );

        for (PointTransaction expiredTx : expiredTransactions) {

            // 2. 유저 조회 (비관적 락)
            User user = userRepository.findWithLockById(expiredTx.getUserId())
                    .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

            // 3. 실제 소멸할 포인트 계산
            // (잔액보다 remainingPoints가 클 수 없음)
            int expireAmount = Math.min(
                    expiredTx.getRemainingPoints(),
                    user.getPointBalance()
            );

            if (expireAmount <= 0) continue;

            // 4. User 잔액 차감
            user.usePoint(expireAmount);

            // 5. 만료된 거래의 remainingPoints 0으로 초기화
            expiredTx.deductRemaining(expireAmount);

            // 6. EXPIRE 타입 거래 이력 기록
            PointTransaction expireTx = PointTransaction.of(
                    expiredTx.getUserId(),
                    null,                 // 주문과 무관
                    -expireAmount,               // 소멸이므로 음수
                    user.getPointBalance(),      // 소멸 후 잔액 스냅샷
                    PointTransactionType.EXPIRE,
                    0,              // EXPIRE 타입은 remainingPoints = 0
                    null                         // EXPIRE 타입은 만료일 없음
            );
            pointRepository.save(expiredTx);

            log.info("포인트 소멸 처리 완료 - userId: {}, amount: {}",
                    expiredTx.getUserId(), expireAmount);
        }

        log.info("포인트 소멸 스케줄러 완료");
    }
}
