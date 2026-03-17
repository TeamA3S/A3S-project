package com.example.a3sproject.domain.point.repository;

import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.point.entity.PointTransaction;
import com.example.a3sproject.domain.point.enums.PointTransactionType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PointRepository extends JpaRepository<PointTransaction, Long> {

    List<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<PointTransaction> findByOrderId(Long orderId);

    List<PointTransaction> findByUserIdAndType(Long userId, PointTransactionType type);

    List<PointTransaction> findByUserIdAndExpiredAtBefore(Long userId, LocalDateTime expiredAtBefore);

    // 동시성 문제를 대비해서 비관적 락을 생성
    @Lock(LockModeType.PESSIMISTIC_WRITE)

    // 무한 대기를 방지하기 위해서 락 대기 시간을 제한
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    Optional<Membership> findByUserId(Long userId);
}
