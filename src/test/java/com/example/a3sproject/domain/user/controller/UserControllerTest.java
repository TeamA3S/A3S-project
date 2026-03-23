package com.example.a3sproject.domain.user.controller;

import com.example.a3sproject.domain.membership.enums.MembershipGrade;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc // Security 필터 정상 작동 (addFilters = false 사용 안 함)
@Transactional
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    // =========================================================
    // POST /api/users/signUp
    // =========================================================

    @Test
    @DisplayName("신규 이메일로 회원가입 API를 호출하면 201 Created와 함께 가입된 회원 정보를 반환한다")
    void createUser_정상요청_201응답() throws Exception {
        // given: SignupUserRequest의 JSON 키는 @JsonProperty 기준 (name, email, password, phone)
        Map<String, String> request = Map.of(
                "name", "신규유저",
                "email", "new@test.com",
                "password", "password123",
                "phone", "010-1111-2222"
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/users/signUp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: 201 Created, 응답 DTO 필드명은 SignupUserResponse 기준 (userName, userEmail 등)
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userEmail").value("new@test.com"))
                .andExpect(jsonPath("$.data.userName").value("신규유저"));
        assertThat(userRepository.existsByEmail("new@test.com")).isTrue();
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 가입을 시도하면 409 상태코드와 USER_ALREADY_EXISTS 예외 코드를 반환한다")
    void createUser_이메일중복_409에러응답() throws Exception {
        // given
        userRepository.save(new User("기존유저", "exist@test.com",
                passwordEncoder.encode("pass"), "010-0000-0000", 0, 0, MembershipGrade.NORMAL, "CUST_001"));
        Map<String, String> request = Map.of(
                "name", "중복유저",
                "email", "exist@test.com",
                "password", "password123",
                "phone", "010-1111-2222"
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/users/signUp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("이메일 필드를 비운 채 가입을 시도하면 400 Bad Request를 반환한다")
    void createUser_이메일누락_400에러응답() throws Exception {
        // given: SignupUserRequest의 @NotBlank(email) 검증 조건을 위반
        Map<String, String> request = Map.of(
                "name", "이메일없는유저",
                "password", "password123",
                "phone", "010-1111-2222"
                // "email" 필드 의도적으로 누락
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/users/signUp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then: @Valid 검증 실패 → GlobalExceptionHandler → 400
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이메일 형식이 잘못된 값으로 가입을 시도하면 400 Bad Request를 반환한다")
    void createUser_잘못된이메일형식_400에러응답() throws Exception {
        // given: SignupUserRequest의 @Email 검증 조건을 위반
        Map<String, String> request = Map.of(
                "name", "형식오류유저",
                "email", "not-an-email",
                "password", "password123",
                "phone", "010-1111-2222"
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/users/signUp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("전화번호 형식이 잘못된 값으로 가입을 시도하면 400 Bad Request를 반환한다")
    void createUser_잘못된전화번호형식_400에러응답() throws Exception {
        // given: SignupUserRequest의 @Pattern(phone) 검증 조건을 위반
        // 올바른 형식: 010-XXXX-XXXX
        Map<String, String> request = Map.of(
                "name", "전화번호오류유저",
                "email", "phone@test.com",
                "password", "password123",
                "phone", "01012345678" // 하이픈 없는 형식
        );

        // when
        ResultActions result = mockMvc.perform(post("/api/users/signUp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    // =========================================================
    // GET /api/users/me
    // =========================================================

    @Test
    @DisplayName("인증된 상태로 내 프로필 조회 API를 호출하면 200 OK와 함께 유저 정보를 반환한다")
    void getMyProfile_인증상태정상요청_200응답() throws Exception {
        // given: 테스트용 유저 저장 후 실제 Access Token 발급
        userRepository.save(new User("프로필유저", "profile@test.com",
                passwordEncoder.encode("pass"), "010-9999-9999", 5000, 0, MembershipGrade.NORMAL, "CUST_002"));
        String accessToken = jwtTokenProvider.createToken("profile@test.com", MembershipGrade.NORMAL);

        // when: Authorization 헤더에 실제 토큰을 담아 요청 (실제 클라이언트 흐름과 동일)
        ResultActions result = mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON));

        // then: UserProfileResponseDto 필드명 기준으로 검증
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("profile@test.com"))
                .andExpect(jsonPath("$.data.name").value("프로필유저"))
                .andExpect(jsonPath("$.data.pointBalance").value(5000))
                .andExpect(jsonPath("$.data.phoneNumber").value("010-9999-9999"))
                .andExpect(jsonPath("$.data.customerUid").exists());
    }

    @Test
    @DisplayName("Authorization 헤더 없이 내 프로필 조회를 시도하면 401 Unauthorized를 반환한다")
    void getMyProfile_비인증요청_401에러응답() throws Exception {
        // given: 토큰을 헤더에 담지 않은 요청
        // Security 필터가 실제로 동작하므로 (addFilters = false 미사용) 인증 실패가 발생한다

        // when: Authorization 헤더 없이 요청
        ResultActions result = mockMvc.perform(get("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON));

        // then: Spring Security가 인증 실패를 감지해 401 반환
        result.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 내 프로필 조회를 시도하면 401 Unauthorized를 반환한다")
    void getMyProfile_위조된토큰_401에러응답() throws Exception {
        // given: 서명이 위조된 토큰
        String invalidToken = "Bearer eyJhbGciOiJIUzUxMiJ9.invalid.signature";

        // when
        ResultActions result = mockMvc.perform(get("/api/users/me")
                .header("Authorization", invalidToken)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isUnauthorized());
    }
}