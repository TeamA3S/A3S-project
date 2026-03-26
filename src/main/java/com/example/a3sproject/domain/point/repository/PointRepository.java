package com.example.a3sproject.domain.point.repository;

import com.example.a3sproject.domain.point.entity.PointTransaction;
import com.example.a3sproject.domain.point.enums.PointTransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PointRepository extends JpaRepository<PointTransaction, Long> {

    List<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 적립 포인트 조회
    List<PointTransaction> findByOrderIdAndType(Long orderId, PointTransactionType type);

    List<PointTransaction> findByUserIdAndOrderIdAndTypeIn(Long userId, Long orderId, List<PointTransactionType> types);

    // FIFO 조회 추가 (만료일 빠른 순서, remainingPoints > 0)
    List<PointTransaction> findByUserIdAndTypeAndRemainingPointsGreaterThanOrderByExpiredAtAsc(
            Long userId,
            PointTransactionType type,
            int remainingPoints
    );

    // 만료 대상의Id 를 10개씩 조회 (잔여 포인트 > 0 인 것들만)
    @Query("""
        SELECT p.id
        FROM PointTransaction p
        WHERE p.type = :type
          AND p.expiredAt <= :now
          AND p.remainingPoints > :minPoints
        ORDER BY p.expiredAt ASC, p.id ASC
    """)
    List<Long> findExpiredTransactionIds(
            @Param("type") PointTransactionType type,
            @Param("now") LocalDateTime now,
            @Param("minPoints") int minPoints,
            Pageable pageable
    );

    // 배치 서비스에서 ID로 재조회, 정렬조건은 만료일과 id
    @Query("""
        SELECT p
        FROM PointTransaction p
        WHERE p.id IN :ids
        ORDER BY p.expiredAt ASC, p.id ASC
    """)
    List<PointTransaction> findAllByIdInOrderByExpiredAtAscIdAsc(@Param("ids") List<Long> ids);
}
