package com.example.a3sproject.domain.point.service;

import com.example.a3sproject.domain.point.entity.PointTransaction;
import com.example.a3sproject.domain.point.enums.PointTransactionType;
import com.example.a3sproject.domain.point.repository.PointRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.exception.domain.UserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PointExpireBatchServiceTest {

    @Mock
    private PointRepository pointRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PointExpireBatchService pointExpireBatchService;

    @Test
    @DisplayName("잔액이 충분하면 포인트를 차감하고 EXPIRE 이력을 저장한다")
    void processChunk_success() {
        // given
        Long txId = 1L;
        Long userId = 10L;

        PointTransaction expiredTx = mock(PointTransaction.class);
        User user = mock(User.class);

        given(pointRepository.findAllByIdInOrderByExpiredAtAscIdAsc(List.of(txId)))
                .willReturn(List.of(expiredTx));

        given(expiredTx.getId()).willReturn(txId);
        given(expiredTx.getUserId()).willReturn(userId);
        given(expiredTx.getRemainingPoints()).willReturn(100);

        given(userRepository.findWithLockById(userId)).willReturn(Optional.of(user));

        // 첫 번째 호출: expireAmount 계산용 / 두 번째 호출: EXPIRE 이력 생성용
        given(user.getPointBalance()).willReturn(500, 400);

        // when
        int processedCount = pointExpireBatchService.processChunk(List.of(txId));

        // then
        assertThat(processedCount).isEqualTo(1);

        then(user).should().usePoint(100);
        then(expiredTx).should().deductRemaining(100);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PointTransaction>> captor = ArgumentCaptor.forClass(List.class);
        then(pointRepository).should().saveAll(captor.capture());

        List<PointTransaction> savedExpireHistory = captor.getValue();
        assertThat(savedExpireHistory).hasSize(1);

        PointTransaction expireTx = savedExpireHistory.get(0);
        assertThat(expireTx.getUserId()).isEqualTo(userId);
        assertThat(expireTx.getPoints()).isEqualTo(-100);
        assertThat(expireTx.getPointBalance()).isEqualTo(400);
        assertThat(expireTx.getType()).isEqualTo(PointTransactionType.EXPIRE);
    }

    @Test
    @DisplayName("유저 잔액이 0이면 EXPIRE 이력은 저장하지 않고 remainingPoints만 정리한다")
    void processChunk_whenUserBalanceZero() {
        // given
        Long txId = 1L;
        Long userId = 10L;

        PointTransaction expiredTx = mock(PointTransaction.class);
        User user = mock(User.class);

        given(pointRepository.findAllByIdInOrderByExpiredAtAscIdAsc(List.of(txId)))
                .willReturn(List.of(expiredTx));

        given(expiredTx.getId()).willReturn(txId);
        given(expiredTx.getUserId()).willReturn(userId);
        given(expiredTx.getRemainingPoints()).willReturn(300);

        given(userRepository.findWithLockById(userId)).willReturn(Optional.of(user));
        given(user.getPointBalance()).willReturn(0);

        // when
        int processedCount = pointExpireBatchService.processChunk(List.of(txId));

        // then
        assertThat(processedCount).isEqualTo(1);

        then(user).should(never()).usePoint(anyInt());
        then(expiredTx).should().deductRemaining(300);
        then(pointRepository).should(never()).saveAll(any());
    }

    @Test
    @DisplayName("유저가 없으면 예외가 발생한다")
    void processChunk_userNotFound() {
        // given
        Long txId = 1L;
        Long userId = 10L;

        PointTransaction expiredTx = mock(PointTransaction.class);

        given(pointRepository.findAllByIdInOrderByExpiredAtAscIdAsc(List.of(txId)))
                .willReturn(List.of(expiredTx));

        given(expiredTx.getUserId()).willReturn(userId);

        given(userRepository.findWithLockById(userId)).willReturn(Optional.empty());

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(
                UserException.class,
                () -> pointExpireBatchService.processChunk(List.of(txId))
        );
    }
}