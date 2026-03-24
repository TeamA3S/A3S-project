package com.example.a3sproject.global.controller;

import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.membership.enums.MembershipGrade;
import com.example.a3sproject.domain.membership.repository.MembershipRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.security.JwtTokenProvider;
import com.example.a3sproject.global.security.refreshtoken.entity.RefreshToken;
import com.example.a3sproject.global.security.refreshtoken.repository.RefreshTokenRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    // =========================================================
    // 공통 헬퍼: 테스트용 유저 + 멤버십 DB 저장
    // =========================================================
    private User saveUserWithMembership(String name, String email, String rawPassword, String phone, String custUid) {
        User user = userRepository.save(
                new User(name, email, passwordEncoder.encode(rawPassword), phone, 0, 0, MembershipGrade.NORMAL, custUid));
        membershipRepository.save(Membership.init(user));
        return user;
    }

    // =========================================================
    // POST /api/auth/login
    // =========================================================

    @Test
    @DisplayName("올바른 이메일과 비밀번호로 로그인하면 200 OK와 Access/Refresh 토큰을 반환한다")
    void login_정상입력_200응답및토큰반환() throws Exception {
        // given
        saveUserWithMembership("김로그인", "login@test.com", "password123", "010-9999-9999", "CUST_LOGIN");
        Map<String, String> request = Map.of("email", "login@test.com", "password", "password123");

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(header().string("Authorization", Matchers.startsWith("Bearer ")))
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.email").value("login@test.com"));
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 401 상태코드와 USER_INFO_MISMATCH 예외를 반환한다")
    void login_비밀번호불일치_401에러응답() throws Exception {
        // given
        saveUserWithMembership("김로그인", "login@test.com", "password123", "010-9999-9999", "CUST_LOGIN");
        Map<String, String> request = Map.of("email", "login@test.com", "password", "wrongPassword");

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_INFO_MISMATCH"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인하면 404 상태코드와 USER_NOT_FOUND 예외를 반환한다")
    void login_존재하지않는이메일_404에러응답() throws Exception {
        // given: DB에 유저가 없는 상태에서 로그인 시도
        // 이메일 자체가 없는 경우는 비밀번호 불일치와 별개의 시나리오
        Map<String, String> request = Map.of("email", "nobody@test.com", "password", "password123");

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("이메일 형식이 잘못된 요청으로 로그인하면 400 Bad Request를 반환한다")
    void login_이메일형식오류_400에러응답() throws Exception {
        // given: LoginRequestDto의 @Email 검증 조건을 위반하는 입력
        Map<String, String> request = Map.of("email", "not-an-email", "password", "password123");

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    // =========================================================
    // POST /api/auth/reissue
    // =========================================================

    @Test
    @DisplayName("유효한 Refresh Token으로 재발급 API를 호출하면 200 OK와 새로 갱신된 토큰을 반환한다")
    void reissue_정상요청_200응답및토큰로테이션() throws Exception {
        // given
        saveUserWithMembership("김재발급", "reissue@test.com", "password123", "010-9999-9999", "CUST_REISSUE");
        String oldRefreshToken = jwtTokenProvider.createRefreshToken("reissue@test.com");
        refreshTokenRepository.save(
                new RefreshToken("reissue@test.com", oldRefreshToken, LocalDateTime.now().plusDays(7)));

        // 토큰 생성 시간 차이를 만들어 새 토큰이 다른 값임을 보장
        Thread.sleep(1000);
        Map<String, String> request = Map.of("refreshToken", oldRefreshToken);

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: 헤더에 새 Access Token, 바디에 기존과 다른 새 Refresh Token 반환
        result.andExpect(status().isOk())
                .andExpect(header().string("Authorization", Matchers.startsWith("Bearer ")))
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").value(Matchers.not(oldRefreshToken)))
                .andExpect(jsonPath("$.data.email").value("reissue@test.com"));
    }

    @Test
    @DisplayName("DB에 존재하지 않는 Refresh Token으로 재발급을 시도하면 401을 반환한다")
    void reissue_DB에없는토큰_401에러응답() throws Exception {
        // given: 유저는 존재하지만 해당 토큰은 DB에 저장되지 않은 상태
        // 예) 로그아웃 후 탈취된 토큰으로 재발급을 시도하는 시나리오
        saveUserWithMembership("김탈취", "stolen@test.com", "password123", "010-1234-5678", "CUST_STOLEN");
        String orphanToken = jwtTokenProvider.createRefreshToken("stolen@test.com");
        // DB에 저장하지 않음 → findByToken() → empty → USER_UNAUTHORIZED 예외 발생
        Map<String, String> request = Map.of("refreshToken", orphanToken);

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("서명이 위조된 Refresh Token으로 재발급을 시도하면 401을 반환한다")
    void reissue_위조된토큰_401에러응답() throws Exception {
        // given: 서명 부분을 임의로 변조한 토큰
        // JwtTokenProvider.validateToken()에서 SignatureException이 발생해야 한다
        String tamperedToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJoYWNrZXJAdGVzdC5jb20ifQ.INVALID_SIGNATURE";
        Map<String, String> request = Map.of("refreshToken", tamperedToken);

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isUnauthorized());
    }

    // =========================================================
    // POST /api/auth/logout
    // =========================================================

    @Test
    @DisplayName("로그아웃 API를 호출하면 200 OK와 함께 DB에서 해당 유저의 Refresh Token이 삭제된다")
    void logout_정상요청_200응답및토큰삭제() throws Exception {
        // given
        refreshTokenRepository.save(
                new RefreshToken("logout@test.com", "dummyRefreshToken", LocalDateTime.now().plusDays(7)));

        // when: Principal을 직접 주입하여 로그아웃 호출 (addFilters=false이므로 Security 필터 우회)
        ResultActions result = mockMvc.perform(post("/api/auth/logout")
                .principal(new UsernamePasswordAuthenticationToken("logout@test.com", null))
                .contentType(MediaType.APPLICATION_JSON));

        // then: 200 OK 반환 및 DB에서 토큰 삭제 여부 검증
        result.andExpect(status().isOk());
        assertThat(refreshTokenRepository.findByToken("dummyRefreshToken")).isEmpty();
    }

    @Test
    @DisplayName("이미 로그아웃된 유저가 재로그아웃해도 200 OK를 반환한다 (멱등성 보장)")
    void logout_토큰없는유저_200응답_멱등성() throws Exception {
        // given: DB에 해당 이메일의 토큰이 없는 상태
        // deleteByEmail()은 대상이 없어도 예외를 던지지 않아야 한다

        // when
        ResultActions result = mockMvc.perform(post("/api/auth/logout")
                .principal(new UsernamePasswordAuthenticationToken("neverLoggedIn@test.com", null))
                .contentType(MediaType.APPLICATION_JSON));

        // then: 예외 없이 200 OK 반환 (멱등성)
        result.andExpect(status().isOk());
    }
}