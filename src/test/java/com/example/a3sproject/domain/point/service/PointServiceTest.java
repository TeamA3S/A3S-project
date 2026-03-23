package com.example.a3sproject.domain.point.service;

import com.example.a3sproject.domain.membership.enums.MembershipGrade;
import com.example.a3sproject.domain.point.dto.MyPointTransactionResponseDto;
import com.example.a3sproject.domain.point.entity.PointTransaction;
import com.example.a3sproject.domain.point.enums.PointTransactionType;
import com.example.a3sproject.domain.point.repository.PointRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.PointException;
import com.example.a3sproject.global.exception.domain.UserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock private PointRepository pointRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private PointService pointService;

    private User createUser(Long id, int pointBalance) {
        User user = new User("홍길동", "test@test.com", "pass", "010-0000-0000",
                pointBalance, 0, MembershipGrade.NORMAL, "CUST_001");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private PointTransaction createPointTransaction(Long id, Long userId, Long orderId,
                                                    int points, int remainingPoints,
                                                    PointTransactionType type,
                                                    LocalDateTime expiredAt) {
        PointTransaction tx = PointTransaction.of(userId, orderId, points, 0, type, remainingPoints, expiredAt);
        ReflectionTestUtils.setField(tx, "id", id);
        return tx;
    }

    // =========================================================
    // validateAndUse - 포인트 차감 (FIFO)
    // =========================================================

    @Test
    @DisplayName("포인트 잔액이 충분하면 FIFO 순서로 포인트가 차감되고 USE 거래 내역이 저장된다")
    void validateAndUse_잔액충분_FIFO차감및USE내역저장() {
        // given: 포인트 잔액 10,000P 유저, 만료일 빠른 순으로 두 건의 EARN 내역 존재
        User user = createUser(1L, 10000);
        PointTransaction earn1 = createPointTransaction(1L, 1L, 1L, 3000, 3000,
                PointTransactionType.EARN, LocalDateTime.now().plusDays(10));
        PointTransaction earn2 = createPointTransaction(2L, 1L, 2L, 7000, 7000,
                PointTransactionType.EARN, LocalDateTime.now().plusDays(30));

        given(userRepository.findWithLockById(1L)).willReturn(Optional.of(user));
        given(pointRepository.findByUserIdAndTypeAndRemainingPointsGreaterThanOrderByExpiredAtAsc(
                1L, PointTransactionType.EARN, 0)).willReturn(List.of(earn1, earn2));
        given(pointRepository.save(any(PointTransaction.class))).willAnswer(i -> i.getArgument(0));

        // when: 5,000P 사용 요청
        pointService.validateAndUse(1L, 10L, 5000);

        // then: 만료일 빠른 earn1에서 먼저 3,000P 차감, earn2에서 나머지 2,000P 차감
        assertThat(earn1.getRemainingPoints()).isZero();
        assertThat(earn2.getRemainingPoints()).isEqualTo(5000);
        assertThat(user.getPointBalance()).isEqualTo(5000);

        ArgumentCaptor<PointTransaction> txCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getType()).isEqualTo(PointTransactionType.USE);
        assertThat(txCaptor.getValue().getPoints()).isEqualTo(-5000);
    }

    @Test
    @DisplayName("포인트 잔액이 부족하면 POINT_NOT_ENOUGH 예외가 발생한다")
    void validateAndUse_잔액부족_POINT_NOT_ENOUGH() {
        // given: 포인트 잔액이 3,000P인데 5,000P 사용 시도
        User user = createUser(1L, 3000);
        given(userRepository.findWithLockById(1L)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> pointService.validateAndUse(1L, 10L, 5000))
                .isInstanceOf(PointException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POINT_NOT_ENOUGH);
    }

    @Test
    @DisplayName("포인트를 전액 사용하면 잔액이 0이 된다")
    void validateAndUse_전액사용_잔액0() {
        // given: 잔액과 동일한 금액 사용 요청
        User user = createUser(1L, 5000);
        PointTransaction earn = createPointTransaction(1L, 1L, 1L, 5000, 5000,
                PointTransactionType.EARN, LocalDateTime.now().plusDays(10));

        given(userRepository.findWithLockById(1L)).willReturn(Optional.of(user));
        given(pointRepository.findByUserIdAndTypeAndRemainingPointsGreaterThanOrderByExpiredAtAsc(
                1L, PointTransactionType.EARN, 0)).willReturn(List.of(earn));
        given(pointRepository.save(any(PointTransaction.class))).willAnswer(i -> i.getArgument(0));

        // when
        pointService.validateAndUse(1L, 10L, 5000);

        // then: 잔액이 0이 되고 remainingPoints도 0으로 소진된다
        assertThat(user.getPointBalance()).isZero();
        assertThat(earn.getRemainingPoints()).isZero();
    }

    // =========================================================
    // earnPoint - 포인트 적립
    // =========================================================

    @Test
    @DisplayName("결제 완료 시 포인트가 적립되고 잔액이 증가하며 EARN 거래 내역이 저장된다")
    void earnPoint_정상적립_잔액증가및EARN내역저장() {
        // given: 포인트 잔액 1,000P 유저에게 500P 적립
        User user = createUser(1L, 1000);
        given(userRepository.findWithLockById(1L)).willReturn(Optional.of(user));
        given(pointRepository.save(any(PointTransaction.class))).willAnswer(i -> i.getArgument(0));

        // when
        pointService.earnPoint(1L, 10L, 500);

        // then: 잔액이 1,500P로 증가하고 EARN 타입 거래 내역이 저장된다
        assertThat(user.getPointBalance()).isEqualTo(1500);

        ArgumentCaptor<PointTransaction> txCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getType()).isEqualTo(PointTransactionType.EARN);
        assertThat(txCaptor.getValue().getPoints()).isEqualTo(500);
        assertThat(txCaptor.getValue().getRemainingPoints()).isEqualTo(500);
    }

    @Test
    @DisplayName("적립된 포인트의 만료일은 적립일로부터 1년 후로 설정된다")
    void earnPoint_만료일_1년후설정() {
        // given: 포인트 적립 요청
        User user = createUser(1L, 0);
        given(userRepository.findWithLockById(1L)).willReturn(Optional.of(user));
        given(pointRepository.save(any(PointTransaction.class))).willAnswer(i -> i.getArgument(0));

        // when
        pointService.earnPoint(1L, 10L, 1000);

        // then: 저장된 거래의 만료일이 오늘로부터 약 1년 후(364~366일)로 설정된다
        ArgumentCaptor<PointTransaction> txCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointRepository).save(txCaptor.capture());
        LocalDateTime expiredAt = txCaptor.getValue().getExpiredAt();
        assertThat(expiredAt).isAfter(LocalDateTime.now().plusDays(364));
        assertThat(expiredAt).isBefore(LocalDateTime.now().plusDays(366));
    }

    // =========================================================
    // restorePoint - 포인트 복구 (결제 실패 시)
    // =========================================================

    @Test
    @DisplayName("결제 실패 시 사용했던 포인트가 복구되고 RESTORE 거래 내역이 저장된다")
    void restorePoint_정상복구_잔액증가및RESTORE내역저장() {
        // given: 포인트 잔액 0P 유저에게 3,000P 복구
        User user = createUser(1L, 0);
        given(userRepository.findWithLockById(1L)).willReturn(Optional.of(user));
        given(pointRepository.save(any(PointTransaction.class))).willAnswer(i -> i.getArgument(0));

        // when
        pointService.restorePoint(1L, 10L, 3000);

        // then: 잔액이 3,000P로 복구되고 RESTORE 타입 거래 내역이 저장된다
        assertThat(user.getPointBalance()).isEqualTo(3000);

        ArgumentCaptor<PointTransaction> txCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getType()).isEqualTo(PointTransactionType.RESTORE);
        assertThat(txCaptor.getValue().getPoints()).isEqualTo(3000);
        assertThat(txCaptor.getValue().getRemainingPoints()).isZero();
    }

    @Test
    @DisplayName("복구된 포인트 거래의 만료일은 null이다")
    void restorePoint_만료일_null() {
        // given: RESTORE 타입은 만료일이 없어야 한다
        User user = createUser(1L, 0);
        given(userRepository.findWithLockById(1L)).willReturn(Optional.of(user));
        given(pointRepository.save(any(PointTransaction.class))).willAnswer(i -> i.getArgument(0));

        // when
        pointService.restorePoint(1L, 10L, 1000);

        // then: 저장된 RESTORE 거래의 만료일이 null이다
        ArgumentCaptor<PointTransaction> txCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getExpiredAt()).isNull();
    }

    // =========================================================
    // cancelEarnedPoint - 적립 포인트 취소 (환불 시)
    // =========================================================

    @Test
    @DisplayName("환불 시 기존에 적립된 포인트가 취소되고 CANCEL 거래 내역이 저장된다")
    void cancelEarnedPoint_정상취소_잔액감소및CANCEL내역저장() {
        // given: 포인트 잔액 5,000P 유저의 적립 포인트 500P 취소
        User user = createUser(1L, 5000);
        given(userRepository.findWithLockById(1L)).willReturn(Optional.of(user));
        given(pointRepository.save(any(PointTransaction.class))).willAnswer(i -> i.getArgument(0));

        // when
        pointService.cancelEarnedPoint(1L, 10L, 500);

        // then: 잔액이 4,500P로 감소하고 CANCEL 타입 거래 내역이 저장된다
        assertThat(user.getPointBalance()).isEqualTo(4500);

        ArgumentCaptor<PointTransaction> txCaptor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getType()).isEqualTo(PointTransactionType.CANCEL);
        assertThat(txCaptor.getValue().getPoints()).isEqualTo(-500);
    }

    // =========================================================
    // getMyPointTransactions - 포인트 거래 내역 조회
    // =========================================================

    @Test
    @DisplayName("내 포인트 거래 내역 조회 시 최신순으로 정렬된 거래 내역 DTO가 반환된다")
    void getMyPointTransactions_정상조회_최신순반환() {
        // given: 유저의 포인트 거래 내역 3건 존재
        User user = createUser(1L, 5000);
        user = new User("홍길동", "test@test.com", "pass", "010-0000-0000",
                5000, 0, MembershipGrade.NORMAL, "CUST_001");
        ReflectionTestUtils.setField(user, "id", 1L);

        PointTransaction earn = createPointTransaction(1L, 1L, 1L, 1000, 1000,
                PointTransactionType.EARN, LocalDateTime.now().plusYears(1));
        PointTransaction use = createPointTransaction(2L, 1L, 2L, -500, 0,
                PointTransactionType.USE, null);
        PointTransaction restore = createPointTransaction(3L, 1L, 2L, 500, 0,
                PointTransactionType.RESTORE, null);

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(pointRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .willReturn(List.of(restore, use, earn));

        // when
        List<MyPointTransactionResponseDto> result = pointService.getMyPointTransactions("test@test.com");

        // then: 3건이 반환되며 최신순(restore → use → earn) 순서가 유지된다
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getType()).isEqualTo(PointTransactionType.RESTORE);
        assertThat(result.get(1).getType()).isEqualTo(PointTransactionType.USE);
        assertThat(result.get(2).getType()).isEqualTo(PointTransactionType.EARN);
    }

    @Test
    @DisplayName("포인트 거래 내역이 없으면 빈 리스트를 반환한다")
    void getMyPointTransactions_내역없음_빈리스트반환() {
        // given: 거래 내역이 없는 유저
        User user = createUser(1L, 0);
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(pointRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of());

        // when
        List<MyPointTransactionResponseDto> result = pointService.getMyPointTransactions("test@test.com");

        // then: 빈 리스트가 반환된다
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 거래 내역 조회 시 USER_NOT_FOUND 예외가 발생한다")
    void getMyPointTransactions_존재하지않는이메일_USER_NOT_FOUND() {
        // given: DB에 없는 이메일
        given(userRepository.findByEmail("nobody@test.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointService.getMyPointTransactions("nobody@test.com"))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}