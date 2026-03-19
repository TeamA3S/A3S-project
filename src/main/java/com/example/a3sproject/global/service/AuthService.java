package com.example.a3sproject.global.service;

import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.membership.repository.MembershipRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.dto.LoginRequestDto;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.UserException;
import com.example.a3sproject.global.security.JwtTokenProvider;
import com.example.a3sproject.global.security.refreshtoken.entity.RefreshToken;
import com.example.a3sproject.global.security.refreshtoken.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    // Access Token + Refresh Token + email을 묶어서 반환하는 record
    public record AuthTokenDto(String accessToken, String refreshToken, String email) {}

    @Transactional
    public AuthTokenDto login(LoginRequestDto request) {
        // 1. email로 User 조회
        User user = (User) userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UserException(ErrorCode.USER_UNAUTHORIZED);
        }

        // 3. 멤버십 등급 조회
        Membership membership = membershipRepository.findByUser(user)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 4. Access Token 발급 (membershipGrade 포함)
        String accessToken = jwtTokenProvider.createToken(
                user.getEmail(),
                membership.getGrade()
        );

        // 5. Refresh Token 발급 및 저장 (Rotation)
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        // 기존 토큰 삭제 추가
        refreshTokenRepository.deleteByEmail(user.getEmail());
        refreshTokenRepository.flush();

        refreshTokenRepository.save(
                new RefreshToken(
                        user.getEmail(),
                        refreshToken,
                        jwtTokenProvider.getRefreshTokenExpiresAt()
                )
        );

        return new AuthTokenDto(accessToken, refreshToken, user.getEmail());
    }

    @Transactional
    public AuthTokenDto reissue(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        jwtTokenProvider.validateToken(refreshToken);

        // 2. DB에서 Refresh Token 조회
        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UserException(ErrorCode.USER_UNAUTHORIZED));

        // 3. email로 User 조회 → membershipGrade 최신값 반영
        User user = (User) userRepository.findByEmail(savedToken.getEmail())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 4. 멤버십 등급 조회
        Membership membership = membershipRepository.findByUser(user)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));


        // 5. Refresh Token Rotation
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());
        savedToken.rotate(newRefreshToken, jwtTokenProvider.getRefreshTokenExpiresAt());
        refreshTokenRepository.save(savedToken);

        // 6. 새 Access Token 발급
        String newAccessToken = jwtTokenProvider.createToken(
                user.getEmail(),
                membership.getGrade()
        );

        return new AuthTokenDto(newAccessToken, newRefreshToken, user.getEmail());
    }
}