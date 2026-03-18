package com.example.a3sproject.domain.user.repository;

import com.example.a3sproject.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일 중복체크
    boolean existsByEmail(String email);

    // 이메일로 유저 조회 (로그인 시 사용)
    Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    Optional<User> findWithLockById(Long userId);
}
