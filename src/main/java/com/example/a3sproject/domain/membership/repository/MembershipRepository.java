package com.example.a3sproject.domain.membership.repository;

import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    // 내 멤버십 등급 조회용 추가
    Optional<Membership> findByUser(User user);

    // 등급 갱신 시 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    Optional<Membership> findWithLockByUser(User user);

}
