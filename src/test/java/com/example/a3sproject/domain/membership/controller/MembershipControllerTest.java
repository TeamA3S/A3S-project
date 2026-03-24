package com.example.a3sproject.domain.membership.controller;

import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.membership.enums.MembershipGrade;
import com.example.a3sproject.domain.membership.repository.MembershipRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class MembershipControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private User 테스트유저;
    private String 액세스토큰;

    @BeforeEach
    void setUp() {
        테스트유저 = userRepository.save(
                new User("멤버십테스터", "membership@test.com",
                        passwordEncoder.encode("pass"), "010-1111-2222",
                        0, 0, MembershipGrade.NORMAL, "CUST_MEM_001"));
        membershipRepository.save(Membership.init(테스트유저));
        액세스토큰 = jwtTokenProvider.createToken("membership@test.com", MembershipGrade.NORMAL);
    }

    // =========================================================
    // GET /api/memberships/me
    // =========================================================

    @Test
    @DisplayName("NORMAL 등급 유저가 멤버십 조회 API를 호출하면 200 OK와 등급/적립률 정보를 반환한다")
    void getMyMembership_NORMAL등급_200응답() throws Exception {
        // given: setUp에서 NORMAL 등급 유저 저장 완료

        // when
        ResultActions result = mockMvc.perform(get("/api/memberships/me")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON));

        // then: NORMAL 등급, 총 결제금액 0원, 적립률 1% 반환
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grade").value("NORMAL"))
                .andExpect(jsonPath("$.data.totalPaymentAmount").value(0))
                .andExpect(jsonPath("$.data.earnRate").value(0.01));
    }

    @Test
    @DisplayName("VIP 등급 유저가 멤버십 조회 API를 호출하면 VIP 등급과 5% 적립률이 반환된다")
    void getMyMembership_VIP등급_200응답() throws Exception {
        // given: 총 결제금액 300,000원인 VIP 유저 별도 생성
        User vipUser = userRepository.save(
                new User("VIP유저", "vip@test.com",
                        passwordEncoder.encode("pass"), "010-3333-4444",
                        0, 300000, MembershipGrade.VIP, "CUST_VIP_001"));
        Membership vipMembership = Membership.init(vipUser);
        ReflectionTestUtils.setField(vipMembership, "grade", MembershipGrade.VIP);
        membershipRepository.save(vipMembership);
        String vipToken = jwtTokenProvider.createToken("vip@test.com", MembershipGrade.VIP);

        // when
        ResultActions result = mockMvc.perform(get("/api/memberships/me")
                .header("Authorization", "Bearer " + vipToken)
                .contentType(MediaType.APPLICATION_JSON));

        // then: VIP 등급, 총 결제금액 300,000원, 적립률 5% 반환
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grade").value("VIP"))
                .andExpect(jsonPath("$.data.totalPaymentAmount").value(300000))
                .andExpect(jsonPath("$.data.earnRate").value(0.05));
    }

    @Test
    @DisplayName("VVIP 등급 유저가 멤버십 조회 API를 호출하면 VVIP 등급과 10% 적립률이 반환된다")
    void getMyMembership_VVIP등급_200응답() throws Exception {
        // given: 총 결제금액 500,000원인 VVIP 유저 별도 생성
        User vvipUser = userRepository.save(
                new User("VVIP유저", "vvip@test.com",
                        passwordEncoder.encode("pass"), "010-5555-6666",
                        0, 500000, MembershipGrade.VVIP, "CUST_VVIP_001"));
        Membership vvipMembership = Membership.init(vvipUser);
        ReflectionTestUtils.setField(vvipMembership, "grade", MembershipGrade.VVIP);
        membershipRepository.save(vvipMembership);
        String vvipToken = jwtTokenProvider.createToken("vvip@test.com", MembershipGrade.VVIP);

        // when
        ResultActions result = mockMvc.perform(get("/api/memberships/me")
                .header("Authorization", "Bearer " + vvipToken)
                .contentType(MediaType.APPLICATION_JSON));

        // then: VVIP 등급, 총 결제금액 500,000원, 적립률 10% 반환
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grade").value("VVIP"))
                .andExpect(jsonPath("$.data.totalPaymentAmount").value(500000))
                .andExpect(jsonPath("$.data.earnRate").value(0.10));
    }

    @Test
    @DisplayName("Authorization 헤더 없이 멤버십 조회를 시도하면 401 Unauthorized를 반환한다")
    void getMyMembership_비인증요청_401에러응답() throws Exception {
        // given: 토큰 없는 요청

        // when
        ResultActions result = mockMvc.perform(get("/api/memberships/me")
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("위조된 토큰으로 멤버십 조회를 시도하면 401 Unauthorized를 반환한다")
    void getMyMembership_위조된토큰_401에러응답() throws Exception {
        // given: 서명이 위조된 토큰
        String invalidToken = "Bearer eyJhbGciOiJIUzUxMiJ9.invalid.signature";

        // when
        ResultActions result = mockMvc.perform(get("/api/memberships/me")
                .header("Authorization", invalidToken)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isUnauthorized());
    }
}