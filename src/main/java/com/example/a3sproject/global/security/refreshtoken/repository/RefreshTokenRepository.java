package com.example.a3sproject.global.security.refreshtoken.repository;

import com.example.a3sproject.global.security.refreshtoken.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByEmail(String email);

    Optional<RefreshToken> findByToken(String token);

    void deleteByEmail(String email);
}