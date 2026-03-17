package com.example.a3sproject.domain.membership.repository;

import com.example.a3sproject.domain.membership.entity.Membership;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    // 동시성 문제를 대비해서 비관적 락을 생성
    @Lock(LockModeType.PESSIMISTIC_WRITE)

    // 무한 대기를 방지하기 위해서 락 대기 시간을 제한
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    Optional<Membership> findByUserId(Long userId);
}
