package com.example.a3sproject.domain.user.service;

import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.membership.enums.MembershipGrade;
import com.example.a3sproject.domain.membership.repository.MembershipRepository;
import com.example.a3sproject.domain.user.dto.SignupUserRequest;
import com.example.a3sproject.domain.user.dto.SignupUserResponse;
import com.example.a3sproject.domain.user.dto.UserProfileResponseDto;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.UserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private final User 테스트유저 = new User(
            "홍길동",
            "test@test.com",
            "encodedPassword",
            "010-1234-5678",
            0, 0, MembershipGrade.NORMAL, "CUST_001"
    );

    // =====================================================================
    // createUser()
    // =====================================================================

    @Test
    @DisplayName("신규 이메일로 회원가입하면 유저와 멤버십이 저장되고 회원 정보가 반환된다")
    void createUser_정상입력_회원가입성공() {
        // given: 이메일 중복 없음, 비밀번호 암호화, 유저/멤버십 저장
        SignupUserRequest request = mock(SignupUserRequest.class);
        given(request.getUserEmail()).willReturn("new@test.com");
        given(request.getUserPassword()).willReturn("password123");
        given(request.getUserName()).willReturn("홍길동");
        given(request.getUserPhoneNumber()).willReturn("010-1234-5678");
        given(userRepository.existsByEmail("new@test.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(membershipRepository.save(any(Membership.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        SignupUserResponse result = userService.createUser(request);

        // then: 반환된 이름과 이메일이 입력값과 일치한다
        assertThat(result.getUserName()).isEqualTo("홍길동");
        assertThat(result.getUserEmail()).isEqualTo("new@test.com");
        assertThat(result.getUserPhoneNumber()).isEqualTo("010-1234-5678");
    }

    @Test
    @DisplayName("회원가입 시 비밀번호는 암호화되어 저장된다")
    void createUser_비밀번호_암호화저장() {
        // given: 이메일 중복 없음, 암호화된 비밀번호로 저장
        SignupUserRequest request = mock(SignupUserRequest.class);
        given(request.getUserEmail()).willReturn("new@test.com");
        given(request.getUserPassword()).willReturn("plainPassword");
        given(request.getUserName()).willReturn("홍길동");
        given(request.getUserPhoneNumber()).willReturn("010-1234-5678");
        given(userRepository.existsByEmail("new@test.com")).willReturn(false);
        given(passwordEncoder.encode("plainPassword")).willReturn("$2a$10$encoded...");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(membershipRepository.save(any(Membership.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        userService.createUser(request);

        // then: 저장된 User의 비밀번호는 암호화된 값이다
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("$2a$10$encoded...");
        assertThat(userCaptor.getValue().getPassword()).isNotEqualTo("plainPassword");
    }

    @Test
    @DisplayName("회원가입 시 포인트 잔액과 총 결제 금액은 0으로 초기화된다")
    void createUser_초기포인트_0으로설정() {
        // given: 정상 회원가입 조건
        SignupUserRequest request = mock(SignupUserRequest.class);
        given(request.getUserEmail()).willReturn("new@test.com");
        given(request.getUserPassword()).willReturn("password123");
        given(request.getUserName()).willReturn("홍길동");
        given(request.getUserPhoneNumber()).willReturn("010-1234-5678");
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(membershipRepository.save(any(Membership.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        userService.createUser(request);

        // then: 저장된 User의 초기 포인트와 결제금액은 0이다
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPointBalance()).isZero();
        assertThat(userCaptor.getValue().getTotalPaymentAmount()).isZero();
        assertThat(userCaptor.getValue().getMembershipGrade()).isEqualTo(MembershipGrade.NORMAL);
    }

    @Test
    @DisplayName("회원가입 시 Membership이 NORMAL 등급으로 초기화되어 저장된다")
    void createUser_멤버십_NORMAL등급초기화() {
        // given: 정상 회원가입 조건
        SignupUserRequest request = mock(SignupUserRequest.class);
        given(request.getUserEmail()).willReturn("new@test.com");
        given(request.getUserPassword()).willReturn("password123");
        given(request.getUserName()).willReturn("홍길동");
        given(request.getUserPhoneNumber()).willReturn("010-1234-5678");
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(membershipRepository.save(any(Membership.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        userService.createUser(request);

        // then: 저장된 Membership의 등급은 NORMAL이다
        ArgumentCaptor<Membership> membershipCaptor = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepository).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().getGrade()).isEqualTo(MembershipGrade.NORMAL);
    }

    @Test
    @DisplayName("이미 가입된 이메일로 회원가입하면 USER_ALREADY_EXISTS 예외가 발생한다")
    void createUser_이메일중복_USER_ALREADY_EXISTS() {
        // given: 동일 이메일로 이미 가입된 유저 존재
        SignupUserRequest request = mock(SignupUserRequest.class);
        given(request.getUserEmail()).willReturn("test@test.com");
        given(userRepository.existsByEmail("test@test.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("이메일 중복 시 유저와 멤버십 저장이 발생하지 않는다")
    void createUser_이메일중복_저장호출안됨() {
        // given: 이메일 중복
        SignupUserRequest request = mock(SignupUserRequest.class);
        given(request.getUserEmail()).willReturn("test@test.com");
        given(userRepository.existsByEmail("test@test.com")).willReturn(true);

        // when
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserException.class);

        // then: 저장 메서드가 호출되지 않는다
        verify(userRepository, org.mockito.Mockito.never()).save(any());
        verify(membershipRepository, org.mockito.Mockito.never()).save(any());
    }

    // =====================================================================
    // getMyProfile()
    // =====================================================================

    @Test
    @DisplayName("존재하는 이메일로 내 프로필을 조회하면 이름, 이메일, 전화번호, 포인트가 반환된다")
    void getMyProfile_존재하는이메일_프로필반환() {
        // given: 유저 조회 성공
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(테스트유저));

        // when
        UserProfileResponseDto result = userService.getMyProfile("test@test.com");

        // then: 반환된 프로필이 유저 정보와 일치한다
        assertThat(result.getName()).isEqualTo("홍길동");
        assertThat(result.getEmail()).isEqualTo("test@test.com");
        assertThat(result.getPhoneNumber()).isEqualTo("010-1234-5678");
        assertThat(result.getPointBalance()).isZero();
        assertThat(result.getCustomerUid()).isEqualTo("CUST_001");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 프로필을 조회하면 USER_NOT_FOUND 예외가 발생한다")
    void getMyProfile_존재하지않는이메일_USER_NOT_FOUND() {
        // given: 유저 조회 실패
        given(userRepository.findByEmail("notexist@test.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getMyProfile("notexist@test.com"))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("포인트가 있는 유저의 프로필 조회 시 현재 포인트 잔액이 정확히 반환된다")
    void getMyProfile_포인트있는유저_포인트정확반환() {
        // given: 포인트 잔액이 5000인 유저
        User 포인트유저 = new User("포인트유저", "point@test.com", "encoded", "010-0000-0000",
                5000, 50000, MembershipGrade.VIP, "CUST_002");
        given(userRepository.findByEmail("point@test.com")).willReturn(Optional.of(포인트유저));

        // when
        UserProfileResponseDto result = userService.getMyProfile("point@test.com");

        // then: 포인트 잔액이 5000으로 반환된다
        assertThat(result.getPointBalance()).isEqualTo(5000);
    }
}