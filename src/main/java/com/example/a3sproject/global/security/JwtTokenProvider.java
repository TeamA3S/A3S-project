package com.example.a3sproject.global.security;

import com.example.a3sproject.domain.membership.enums.MembershipGrade;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증 유틸리티
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long tokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    public JwtTokenProvider(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.token-validity-in-seconds}") long tokenValidityInSeconds,
        @Value("${jwt.refresh-token-validity-in-seconds}") long refreshTokenValidityInSeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        System.out.println(secret.length());
        this.tokenValidityInMilliseconds = tokenValidityInSeconds * 1000;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidityInSeconds * 1000;
    }

    /**
     * Refresh Token 생성
     * Claims 없이 subject(email)만 포함 — 최소한의 정보만 담습니다.
     */
    public String createRefreshToken(String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Refresh Token 만료 시각 반환 — DB 저장용
     */
    public LocalDateTime getRefreshTokenExpiresAt() {
        return LocalDateTime.now()
                .plusSeconds(refreshTokenValidityInMilliseconds / 1000);
    }

    /**
     * JWT 토큰 생성
     *
     * - 사용자 멤버십(membership) 정보 추가
     */
    public String createToken(String email, MembershipGrade membershipGrade) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);

        return Jwts.builder()
            .subject(email)
            .claim("membershipGrade", membershipGrade.name())
            .issuedAt(now)
            .expiration(validity)
            .signWith(secretKey)
            .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * JWT 토큰에서 이메일 추출
     */
    public String getEmail(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * JWT 토큰에서 멤버십 등급 추출
     */
    public String getMembership(String token) {
        return getClaims(token).get("membership", String.class);
    }

    /**
     * JWT 토큰 유효성 검증
     * - 토큰 블랙리스트 체크 (로그아웃된 토큰)
     * - 토큰 갱신 로직
     * - 예외를 catch하지 않고 그대로 전파 → JwtAuthenticationFilter에서 처리
     */
    public void validateToken(String token) {
        Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
        // - ExpiredJwtException: 만료된 토큰
        // - MalformedJwtException: 잘못된 형식
        // - SignatureException: 서명 오류
    }
}
