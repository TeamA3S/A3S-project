package com.example.a3sproject.domain.user.repository;

import com.example.a3sproject.domain.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<Object> findByEmail(@NotBlank(message = "이메일을 입력해주세요.") @Email(message = "이메일 형식으로 입력해주세요.") String email);
}
