package com.example.a3sproject.domain.membership.repository;

import com.example.a3sproject.domain.membership.entity.Membership;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {}
