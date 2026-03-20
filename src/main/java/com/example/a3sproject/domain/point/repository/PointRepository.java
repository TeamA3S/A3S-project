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

    // 적립 포인트 조회
    List<PointTransaction> findByOrderIdAndType(Long orderId, PointTransactionType type);

    List<PointTransaction> findByUserIdAndType(Long userId, PointTransactionType type);

    List<PointTransaction> findByUserIdAndExpiredAtBefore(Long userId, LocalDateTime expiredAtBefore);

    List<PointTransaction> findByUserIdAndOrderIdAndTypeIn(Long userId, Long orderId, List<PointTransactionType> types);
}
