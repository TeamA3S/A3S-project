package com.example.a3sproject.domain.point.repository;

import com.example.a3sproject.domain.point.entity.PointTransaction;
import com.example.a3sproject.domain.point.enums.PointTransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PointRepositoryTest {

    @Autowired
    private PointRepository pointRepository;

    @Test
    @DisplayName("만료 대상 ID를 오래된 순으로 최대 10건까지 조회한다")
    void findExpiredTransactionIds_success() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);

        PointTransaction tx1 = pointRepository.save(
                PointTransaction.of(
                        1L, null, 1000, 1000,
                        PointTransactionType.EARN,
                        1000,
                        now.minusDays(3)
                )
        );

        PointTransaction tx2 = pointRepository.save(
                PointTransaction.of(
                        2L, null, 500, 500,
                        PointTransactionType.EARN,
                        500,
                        now.minusDays(2)
                )
        );

        // 제외 대상: 아직 만료 안 됨
        pointRepository.save(
                PointTransaction.of(
                        3L, null, 700, 700,
                        PointTransactionType.EARN,
                        700,
                        now.plusDays(1)
                )
        );

        // 제외 대상: remainingPoints == 0
        pointRepository.save(
                PointTransaction.of(
                        4L, null, 300, 300,
                        PointTransactionType.EARN,
                        0,
                        now.minusDays(5)
                )
        );

        // 제외 대상: 타입이 EXPIRE
        pointRepository.save(
                PointTransaction.of(
                        5L, null, -200, 100,
                        PointTransactionType.EXPIRE,
                        0,
                        now.minusDays(1)
                )
        );

        // when
        List<Long> ids = pointRepository.findExpiredTransactionIds(
                PointTransactionType.EARN,
                now,
                0,
                PageRequest.of(0, 10)
        );

        // then
        assertThat(ids).containsExactly(tx1.getId(), tx2.getId());
    }
}