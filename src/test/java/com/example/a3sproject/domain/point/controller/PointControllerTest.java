package com.example.a3sproject.domain.point.controller;

import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.membership.enums.MembershipGrade;
import com.example.a3sproject.domain.membership.repository.MembershipRepository;
import com.example.a3sproject.domain.point.entity.PointTransaction;
import com.example.a3sproject.domain.point.enums.PointTransactionType;
import com.example.a3sproject.domain.point.repository.PointRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class PointControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private PointRepository pointRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private User 테스트유저;
    private String 액세스토큰;

    @BeforeEach
    void setUp() {
        // 테스트유저 저장, 멤버십 초기화, 액세스 토큰 발급
        테스트유저 = userRepository.save(
                new User("포인트테스터", "point@test.com",
                        passwordEncoder.encode("pass"), "010-1111-2222",
                        10000, 0, MembershipGrade.NORMAL, "CUST_POINT_001"));
        membershipRepository.save(Membership.init(테스트유저));
        액세스토큰 = jwtTokenProvider.createToken("point@test.com", MembershipGrade.NORMAL);
    }

    // =========================================================
    // GET /api/points/me
    // =========================================================

    @Test
    @DisplayName("인증된 유저가 포인트 거래 내역 조회 API를 호출하면 200 OK와 거래 내역 목록을 반환한다")
    void getMyPointTransactions_정상요청_200응답() throws Exception {
        // given: 테스트유저의 포인트 거래 내역 2건 저장
        pointRepository.save(PointTransaction.of(
                테스트유저.getId(), 1L, 1000, 11000,
                PointTransactionType.EARN, 1000,
                LocalDateTime.now().plusYears(1)));
        pointRepository.save(PointTransaction.of(
                테스트유저.getId(), 2L, -500, 10500,
                PointTransactionType.USE, 0, null));

        // when
        ResultActions result = mockMvc.perform(get("/api/points/me")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON));

        // then: 200 반환, 2건의 거래 내역이 포함된 배열이 반환된다
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("포인트 거래 내역 조회 시 각 항목에 타입과 변동액이 포함된다")
    void getMyPointTransactions_정상요청_거래내역필드검증() throws Exception {
        // given: EARN 타입 거래 내역 1건 저장
        pointRepository.save(PointTransaction.of(
                테스트유저.getId(), 1L, 2000, 12000,
                PointTransactionType.EARN, 2000,
                LocalDateTime.now().plusYears(1)));

        // when
        ResultActions result = mockMvc.perform(get("/api/points/me")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON));

        // then: 거래 내역의 type, points, balance 필드가 정확히 반환된다
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("EARN"))
                .andExpect(jsonPath("$.data[0].points").value(2000))
                .andExpect(jsonPath("$.data[0].balance").value(12000));
    }

    @Test
    @DisplayName("포인트 거래 내역이 없으면 200 OK와 빈 배열을 반환한다")
    void getMyPointTransactions_내역없음_200빈배열반환() throws Exception {
        // given: 거래 내역이 없는 상태 (setUp에서 거래 내역 저장 없음)

        // when
        ResultActions result = mockMvc.perform(get("/api/points/me")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON));

        // then: 404가 아닌 200 OK와 빈 배열이 반환된다
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("포인트 거래 내역은 최신순으로 정렬되어 반환된다")
    void getMyPointTransactions_최신순정렬_검증() throws Exception {
        // given: EARN 거래 후 USE 거래가 발생한 상황 (USE가 더 최신)
        pointRepository.save(PointTransaction.of(
                테스트유저.getId(), 1L, 1000, 11000,
                PointTransactionType.EARN, 1000,
                LocalDateTime.now().plusYears(1)));
        Thread.sleep(100);
        pointRepository.save(PointTransaction.of(
                테스트유저.getId(), 2L, -300, 10700,
                PointTransactionType.USE, 0, null));

        // when
        ResultActions result = mockMvc.perform(get("/api/points/me")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON));

        // then: 첫 번째 항목이 USE(최신), 두 번째 항목이 EARN(과거)으로 반환된다
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("USE"))
                .andExpect(jsonPath("$.data[1].type").value("EARN"));
    }

    @Test
    @DisplayName("타유저의 포인트 거래 내역은 내 조회 결과에 포함되지 않는다")
    void getMyPointTransactions_타유저내역분리_본인내역만반환() throws Exception {
        // given: 타유저 생성 후 각각 거래 내역 저장
        User 타유저 = userRepository.save(new User("타유저", "other@test.com",
                passwordEncoder.encode("pass"), "010-7777-8888",
                5000, 0, MembershipGrade.NORMAL, "CUST_OTHER_001"));
        membershipRepository.save(Membership.init(타유저));

        // 타유저 거래 내역 2건
        pointRepository.save(PointTransaction.of(
                타유저.getId(), 1L, 5000, 5000,
                PointTransactionType.EARN, 5000,
                LocalDateTime.now().plusYears(1)));
        pointRepository.save(PointTransaction.of(
                타유저.getId(), 2L, -1000, 4000,
                PointTransactionType.USE, 0, null));

        // 테스트유저 거래 내역 1건
        pointRepository.save(PointTransaction.of(
                테스트유저.getId(), 3L, 1000, 11000,
                PointTransactionType.EARN, 1000,
                LocalDateTime.now().plusYears(1)));

        // when: 테스트유저 토큰으로 조회
        ResultActions result = mockMvc.perform(get("/api/points/me")
                .header("Authorization", "Bearer " + 액세스토큰)
                .contentType(MediaType.APPLICATION_JSON));

        // then: 테스트유저의 거래 내역 1건만 반환되며 타유저 내역은 포함되지 않는다
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].type").value("EARN"));
    }

    @Test
    @DisplayName("Authorization 헤더 없이 포인트 내역 조회를 시도하면 401 Unauthorized를 반환한다")
    void getMyPointTransactions_비인증요청_401에러응답() throws Exception {
        // given: 토큰 없는 요청

        // when
        ResultActions result = mockMvc.perform(get("/api/points/me")
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isUnauthorized());
    }
}