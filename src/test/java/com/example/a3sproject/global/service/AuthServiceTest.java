package com.example.a3sproject.global.service;

import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.membership.enums.MembershipGrade;
import com.example.a3sproject.domain.membership.repository.MembershipRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.dto.LoginRequestDto;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.UserException;
import com.example.a3sproject.global.security.JwtTokenProvider;
import com.example.a3sproject.global.security.refreshtoken.entity.RefreshToken;
import com.example.a3sproject.global.security.refreshtoken.repository.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    private final User 테스트유저 = new User(
            "홍길동",
            "test@test.com",
            "encodedPassword",
            "010-1234-5678",
            0, 0, MembershipGrade.NORMAL, "CUST_001"
    );

    private final Membership 테스트멤버십 = Membership.init(테스트유저);

    // =====================================================================
    // login()
    // =====================================================================

    @Test
    @DisplayName("정상적인 이메일/비밀번호로 로그인하면 Access Token, Refresh Token, 이메일을 반환한다")
    void login_정상입력_토큰반환() {
        // given: 유저 조회 성공, 비밀번호 일치, 멤버십 조회 성공, 토큰 생성
        LoginRequestDto request = mock(LoginRequestDto.class);
        given(request.getEmail()).willReturn("test@test.com");
        given(request.getPassword()).willReturn("password123");
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(테스트유저));
        given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
        given(membershipRepository.findByUser(테스트유저)).willReturn(Optional.of(테스트멤버십));
        given(jwtTokenProvider.createToken("test@test.com", MembershipGrade.NORMAL)).willReturn("accessToken");
        given(jwtTokenProvider.createRefreshToken("test@test.com")).willReturn("refreshToken");
        given(jwtTokenProvider.getRefreshTokenExpiresAt()).willReturn(LocalDateTime.now().plusDays(7));

        // when
        AuthService.AuthTokenDto result = authService.login(request);

        // then: 반환된 토큰과 이메일이 정확히 일치한다
        assertThat(result.accessToken()).isEqualTo("accessToken");
        assertThat(result.refreshToken()).isEqualTo("refreshToken");
        assertThat(result.email()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("로그인 시 기존 Refresh Token을 삭제하고 새로 저장한다 (Token Rotation)")
    void login_토큰로테이션_기존토큰삭제후저장() {
        // given: 정상 로그인 조건
        LoginRequestDto request = mock(LoginRequestDto.class);
        given(request.getEmail()).willReturn("test@test.com");
        given(request.getPassword()).willReturn("password123");
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(테스트유저));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
        given(membershipRepository.findByUser(테스트유저)).willReturn(Optional.of(테스트멤버십));
        given(jwtTokenProvider.createToken(anyString(), any())).willReturn("accessToken");
        given(jwtTokenProvider.createRefreshToken(anyString())).willReturn("refreshToken");
        given(jwtTokenProvider.getRefreshTokenExpiresAt()).willReturn(LocalDateTime.now().plusDays(7));

        // when
        authService.login(request);

        // then: 기존 토큰 삭제 → flush → 새 토큰 저장 순서가 보장된다
        InOrder inOrder = inOrder(refreshTokenRepository);
        inOrder.verify(refreshTokenRepository).deleteByEmail("test@test.com");
        inOrder.verify(refreshTokenRepository).flush();
        inOrder.verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인하면 USER_NOT_FOUND 예외가 발생한다")
    void login_존재하지않는이메일_USER_NOT_FOUND() {
        // given: 유저 조회 실패
        LoginRequestDto request = mock(LoginRequestDto.class);
        given(request.getEmail()).willReturn("notexist@test.com");
        given(userRepository.findByEmail("notexist@test.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 USER_INFO_MISMATCH 예외가 발생한다")
    void login_비밀번호불일치_USER_UNAUTHORIZED() {
        // given: 유저 조회 성공, 비밀번호 불일치
        LoginRequestDto request = mock(LoginRequestDto.class);
        given(request.getEmail()).willReturn("test@test.com");
        given(request.getPassword()).willReturn("wrongPassword");
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(테스트유저));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_INFO_MISMATCH);
    }

    @Test
    @DisplayName("유저는 존재하지만 멤버십이 없으면 USER_NOT_FOUND 예외가 발생한다")
    void login_멤버십없음_USER_NOT_FOUND() {
        // given: 유저 조회 성공, 비밀번호 일치, 멤버십 조회 실패
        LoginRequestDto request = mock(LoginRequestDto.class);
        given(request.getEmail()).willReturn("test@test.com");
        given(request.getPassword()).willReturn("password123");
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(테스트유저));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
        given(membershipRepository.findByUser(테스트유저)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    // =====================================================================
    // reissue()
    // =====================================================================

    @Test
    @DisplayName("유효한 Refresh Token으로 재발급하면 새로운 Access Token과 Refresh Token을 반환한다")
    void reissue_유효한토큰_새토큰반환() {
        // given: 토큰 유효성 통과, DB 토큰 조회 성공, 유저/멤버십 조회 성공
        RefreshToken savedToken = new RefreshToken("test@test.com", "oldRefreshToken", LocalDateTime.now().plusDays(7));
        given(refreshTokenRepository.findByToken("oldRefreshToken")).willReturn(Optional.of(savedToken));
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(테스트유저));
        given(membershipRepository.findByUser(테스트유저)).willReturn(Optional.of(테스트멤버십));
        given(jwtTokenProvider.createRefreshToken("test@test.com")).willReturn("newRefreshToken");
        given(jwtTokenProvider.getRefreshTokenExpiresAt()).willReturn(LocalDateTime.now().plusDays(7));
        given(jwtTokenProvider.createToken("test@test.com", MembershipGrade.NORMAL)).willReturn("newAccessToken");

        // when
        AuthService.AuthTokenDto result = authService.reissue("oldRefreshToken");

        // then: 새 토큰이 반환되고 기존 토큰은 rotate된다
        assertThat(result.accessToken()).isEqualTo("newAccessToken");
        assertThat(result.refreshToken()).isEqualTo("newRefreshToken");
        assertThat(result.email()).isEqualTo("test@test.com");
        verify(refreshTokenRepository).save(savedToken);
    }

    @Test
    @DisplayName("DB에 존재하지 않는 Refresh Token으로 재발급하면 USER_UNAUTHORIZED 예외가 발생한다")
    void reissue_DB에없는토큰_USER_UNAUTHORIZED() {
        // given: 토큰 유효성 통과, DB 토큰 조회 실패
        doNothing().when(jwtTokenProvider).validateToken("unknownToken");
        given(refreshTokenRepository.findByToken("unknownToken")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.reissue("unknownToken"))
                .isInstanceOf(UserException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_UNAUTHORIZED);
    }

    @Test
    @DisplayName("만료된 Refresh Token으로 재발급하면 JwtException이 전파된다")
    void reissue_만료된토큰_JwtException전파() {
        // given: 토큰 유효성 검증 실패 (만료)
        doThrow(new io.jsonwebtoken.ExpiredJwtException(null, null, "만료된 토큰"))
                .when(jwtTokenProvider).validateToken("expiredToken");

        // when & then: validateToken에서 발생한 예외가 그대로 전파된다
        assertThatThrownBy(() -> authService.reissue("expiredToken"))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    @DisplayName("재발급 시 Access Token에 최신 멤버십 등급이 반영된다")
    void reissue_멤버십등급변경_최신등급반영() {
        // given: 멤버십 등급이 VIP로 업그레이드된 상황
        User vipUser = new User("김VIP", "vip@test.com", "encoded", "010-9999-9999",
                100000, 300000, MembershipGrade.VIP, "CUST_VIP");
        Membership vipMembership = Membership.init(vipUser);
        vipMembership.updateGrade(vipUser.getTotalPaymentAmount());

        RefreshToken savedToken = new RefreshToken("vip@test.com", "vipToken", LocalDateTime.now().plusDays(7));
        doNothing().when(jwtTokenProvider).validateToken("vipToken");
        given(refreshTokenRepository.findByToken("vipToken")).willReturn(Optional.of(savedToken));
        given(userRepository.findByEmail("vip@test.com")).willReturn(Optional.of(vipUser));
        given(membershipRepository.findByUser(vipUser)).willReturn(Optional.of(vipMembership));
        given(jwtTokenProvider.createToken("vip@test.com", MembershipGrade.VIP)).willReturn("vipAccessToken");
        given(jwtTokenProvider.createRefreshToken("vip@test.com")).willReturn("newVipRefreshToken");
        given(jwtTokenProvider.getRefreshTokenExpiresAt()).willReturn(LocalDateTime.now().plusDays(7));

        // when
        AuthService.AuthTokenDto result = authService.reissue("vipToken");

        // then: VIP 등급으로 토큰이 생성된다
        verify(jwtTokenProvider).createToken("vip@test.com", MembershipGrade.VIP);
        assertThat(result.accessToken()).isEqualTo("vipAccessToken");
    }

//    @Test
//    @DisplayName("Refresh Token은 유효하지만 해당 유저가 탈퇴했다면 USER_NOT_FOUND 예외가 발생한다")
//    void reissue_유저없음_USER_NOT_FOUND() {
//        // given: 토큰 DB 조회 성공, 유저 조회 실패
//        RefreshToken savedToken = new RefreshToken("deleted@test.com", "validToken", LocalDateTime.now().plusDays(7));
//        doNothing().when(jwtTokenProvider).validateToken("validToken");
//        given(refreshTokenRepository.findByToken("validToken")).willReturn(Optional.of(savedToken));
//        given(userRepository.findByEmail("deleted@test.com")).willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> authService.reissue("validToken"))
//                .isInstanceOf(UserException.class)
//                .extracting("errorCode")
//                .isEqualTo(ErrorCode.USER_NOT_FOUND);

//    }
}