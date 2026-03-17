package com.example.a3sproject.domain.point.repository;

import com.example.a3sproject.domain.point.entity.Point;
import com.example.a3sproject.domain.point.enums.PointTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PointRepository extends JpaRepository<Point, Long> {

    List<Point> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Point> findByOrderId(Long orderId);

    List<Point> findByUserIdAndType(Long userId, PointTransactionType type);

    List<Point> findByUserIdAndExpiredAtBefore(Long userId, LocalDateTime expiredAtBefore);
}
