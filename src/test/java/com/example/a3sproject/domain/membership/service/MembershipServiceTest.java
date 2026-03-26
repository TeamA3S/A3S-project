package com.example.a3sproject.domain.membership.service;

import com.example.a3sproject.domain.membership.dto.MyMembershipResponseDto;
import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.membership.enums.MembershipGrade;
import com.example.a3sproject.domain.membership.repository.MembershipRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.UserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock private MembershipRepository membershipRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private MembershipService membershipService;

    private User createUser(Long id, int pointBalance, int totalPaymentAmount, MembershipGrade grade) {
        User user = new User("홍길동", "test@test.com", "pass", "010-0000-0000",
                pointBalance, totalPaymentAmount, grade, "CUST_001");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Membership createMembership(User user, MembershipGrade grade) {
        Membership membership = createInstance(Membership.class);
        ReflectionTestUtils.setField(membership, "user", user);
        ReflectionTestUtils.setField(membership, "grade", grade);
        return membership;
    }

    private <T> T createInstance(Class<T> clazz) {
        try {
            java.lang.reflect.Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("정상적인 이메일로 멤버십 조회 시 등급, 총 결제금액, 적립률이 반환된다")
    void getMyMembership_정상조회_멤버십정보반환() {
        // given: NORMAL 등급 유저의 멤버십 조회
        User user = createUser(1L, 0, 0, MembershipGrade.NORMAL);
        Membership membership = createMembership(user, MembershipGrade.NORMAL);

        given(membershipRepository.findByUser(user)).willReturn(Optional.of(membership));

        // when
        MyMembershipResponseDto result = membershipService.getMyMembership(user);

        // then: NORMAL 등급, 총 결제금액 0원, 적립률 1% 반환
        assertThat(result.getGrade()).isEqualTo(MembershipGrade.NORMAL);
        assertThat(result.getTotalPaymentAmount()).isEqualTo(0);
        assertThat(result.getEarnRate()).isEqualTo(0.01);
    }

    @Test
    @DisplayName("총 결제금액 300,000원 이상이면 VIP 등급과 5% 적립률이 반환된다")
    void getMyMembership_VIP등급조회_5퍼센트적립률반환() {
        // given: 총 결제금액 300,000원인 VIP 유저
        User user = createUser(1L, 0, 300000, MembershipGrade.VIP);
        Membership membership = createMembership(user, MembershipGrade.VIP);

        given(membershipRepository.findByUser(user)).willReturn(Optional.of(membership));

        // when
        MyMembershipResponseDto result = membershipService.getMyMembership(user);

        // then: VIP 등급, 총 결제금액 300,000원, 적립률 5% 반환
        assertThat(result.getGrade()).isEqualTo(MembershipGrade.VIP);
        assertThat(result.getTotalPaymentAmount()).isEqualTo(300000);
        assertThat(result.getEarnRate()).isEqualTo(0.05);
    }

    @Test
    @DisplayName("총 결제금액 500,000원 이상이면 VVIP 등급과 10% 적립률이 반환된다")
    void getMyMembership_VVIP등급조회_10퍼센트적립률반환() {
        // given: 총 결제금액 500,000원인 VVIP 유저
        User user = createUser(1L, 0, 500000, MembershipGrade.VVIP);
        Membership membership = createMembership(user, MembershipGrade.VVIP);

        given(membershipRepository.findByUser(user)).willReturn(Optional.of(membership));

        // when
        MyMembershipResponseDto result = membershipService.getMyMembership(user);

        // then: VVIP 등급, 총 결제금액 500,000원, 적립률 10% 반환
        assertThat(result.getGrade()).isEqualTo(MembershipGrade.VVIP);
        assertThat(result.getTotalPaymentAmount()).isEqualTo(500000);
        assertThat(result.getEarnRate()).isEqualTo(0.10);
    }

    @Test
    @DisplayName("유저는 존재하지만 멤버십이 없으면 USER_NOT_FOUND 예외가 발생한다")
    void getMyMembership_멤버십없음_USER_NOT_FOUND() {
        // given: 유저는 존재하지만 멤버십 초기화가 누락된 상황
        User user = createUser(1L, 0, 0, MembershipGrade.NORMAL);

        given(membershipRepository.findByUser(user)).willReturn(Optional.empty());

        // when & then: 멤버십 조회 실패 시 USER_NOT_FOUND 예외 발생
        assertThatThrownBy(() -> membershipService.getMyMembership(user))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    // =========================================================
    // Membership 도메인 로직 단위 테스트 (Mock 불필요)
    // =========================================================

    @Test
    @DisplayName("총 결제금액 갱신 후 updateGrade 호출 시 300,000원 미만이면 NORMAL로 유지된다")
    void updateGrade_299999원_NORMAL유지() {
        // given: NORMAL 등급 멤버십, 총 결제금액 299,999원
        User user = createUser(1L, 0, 299999, MembershipGrade.NORMAL);
        Membership membership = createMembership(user, MembershipGrade.NORMAL);

        // when
        membership.updateGrade(299999);

        // then: VIP 조건 미충족으로 NORMAL 유지
        assertThat(membership.getGrade()).isEqualTo(MembershipGrade.NORMAL);
    }

    @Test
    @DisplayName("총 결제금액이 정확히 300,000원이면 VIP로 등급이 갱신된다")
    void updateGrade_300000원_VIP승급() {
        // given: NORMAL 등급 멤버십
        User user = createUser(1L, 0, 0, MembershipGrade.NORMAL);
        Membership membership = createMembership(user, MembershipGrade.NORMAL);

        // when: VIP 경계값인 300,000원으로 등급 갱신
        membership.updateGrade(300000);

        // then: 정확히 경계값에서 VIP로 승급된다
        assertThat(membership.getGrade()).isEqualTo(MembershipGrade.VIP);
    }

    @Test
    @DisplayName("총 결제금액이 정확히 500,000원이면 VVIP로 등급이 갱신된다")
    void updateGrade_500000원_VVIP승급() {
        // given: VIP 등급 멤버십
        User user = createUser(1L, 0, 0, MembershipGrade.VIP);
        Membership membership = createMembership(user, MembershipGrade.VIP);

        // when: VVIP 경계값인 500,000원으로 등급 갱신
        membership.updateGrade(500000);

        // then: 정확히 경계값에서 VVIP로 승급된다
        assertThat(membership.getGrade()).isEqualTo(MembershipGrade.VVIP);
    }

    @Test
    @DisplayName("환불 후 총 결제금액이 줄어들면 등급이 다운그레이드된다")
    void updateGrade_환불후결제금액감소_등급다운그레이드() {
        // given: VVIP 등급인데 환불로 인해 총 결제금액이 250,000원으로 감소
        User user = createUser(1L, 0, 500000, MembershipGrade.VVIP);
        Membership membership = createMembership(user, MembershipGrade.VVIP);

        // when: 환불 후 재계산된 총 결제금액으로 등급 갱신
        membership.updateGrade(250000);

        // then: VVIP → NORMAL로 다운그레이드 (300,000원 미만)
        assertThat(membership.getGrade()).isEqualTo(MembershipGrade.NORMAL);
    }

    @Test
    @DisplayName("등급별 적립률이 정책에 맞게 반환된다")
    void getEarnRate_등급별적립률_정책검증() {
        // given: 각 등급별 멤버십 생성
        User user = createUser(1L, 0, 0, MembershipGrade.NORMAL);

        Membership normalMembership = createMembership(user, MembershipGrade.NORMAL);
        Membership vipMembership = createMembership(user, MembershipGrade.VIP);
        Membership vvipMembership = createMembership(user, MembershipGrade.VVIP);

        // when & then: 각 등급의 적립률이 정책(1%/5%/10%)과 일치한다
        assertThat(normalMembership.getEarnRate()).isEqualTo(0.01);
        assertThat(vipMembership.getEarnRate()).isEqualTo(0.05);
        assertThat(vvipMembership.getEarnRate()).isEqualTo(0.10);
    }
}