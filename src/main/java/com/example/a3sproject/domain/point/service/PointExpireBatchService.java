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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointExpireBatchService {

    private final PointRepository pointRepository;
    private final UserRepository userRepository;

    // 10건 단위 독립 트랜잭션(외부 트랜잭션과 무관하게 항상 새로운 트랜잭션)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int processChunk(List<Long> expiredTxIds) {

        List<PointTransaction> expiredTransactions =
                pointRepository.findAllByIdInOrderByExpiredAtAscIdAsc(expiredTxIds);

        List<PointTransaction> expireHistory = new ArrayList<>();
        int processedCount = 0;

        for (PointTransaction expiredTx : expiredTransactions) {

            // 1) 유저 비관적 락 조회
            User user = userRepository.findWithLockById(expiredTx.getUserId())
                    .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

            int remainingBefore = expiredTx.getRemainingPoints();

            // 2) 실제 소멸할 수 있는 양 계산
            int expireAmount = Math.min(remainingBefore, user.getPointBalance());

            if (expireAmount > 0) {
                // 3) 현재 유저 잔액 차감
                user.usePoint(expireAmount);

                // 4) EXPIRE 거래 이력 생성
                PointTransaction expireTx = PointTransaction.of(
                        expiredTx.getUserId(),
                        null,                       // 주문과 무관
                        -expireAmount,             // 소멸이므로 음수
                        user.getPointBalance(),    // 차감 후 잔액 스냅샷
                        PointTransactionType.EXPIRE,
                        0,
                        null
                );
                expireHistory.add(expireTx);

                log.info("포인트 소멸 처리 - userId: {}, expiredTxId: {}, amount: {}",
                        expiredTx.getUserId(), expiredTx.getId(), expireAmount);
            } else {
                log.warn("소멸 대상이나 실제 차감 불가 - userId: {}, expiredTxId: {}, remainingBefore: {}, userPointBalance: {}",
                        expiredTx.getUserId(), expiredTx.getId(), remainingBefore, user.getPointBalance());
            }
            // 5) 원본 EARN의 remainingPoints 정리
            //    잔액 부족으로 실제 차감이 0이어도, 다시 조회되지 않게 만료 처리 마감
            if (remainingBefore > 0) {
                int cleared = expiredTx.deductRemaining(remainingBefore);

                log.debug("remainingPoints 정리 - expiredTxId: {}, cleared: {}",
                        expiredTx.getId(), cleared);
            }

            processedCount++;
        }
        // 6) EXPIRE 이력 모아서 저장
        if (!expireHistory.isEmpty()) {
            pointRepository.saveAll(expireHistory);
        }
        return processedCount;
    }
}
