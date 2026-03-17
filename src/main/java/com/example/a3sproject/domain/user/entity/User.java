package com.example.a3sproject.domain.user.entity;

import com.example.a3sproject.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    // 사용자 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자 이름
    @Column(nullable = false)
    private String name;

    // 사용자 이메일
    @Column(nullable = false, unique = true)
    private String email;

    // 회원가입과 로그인을 위한 비밀번호 필드 추가
    @Column(nullable = false)
    private String password;

    // 사용자 전화번호
    @Column(nullable = false)
    private String phoneNumber;

    // 포인트 잔액(스냅샷)
    @Column(nullable = false)
    private Integer pointBalance;

    // 회원가입용 생성자
    public User(String name, String email, String password, String phoneNumber, Integer pointBalance ) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.pointBalance = pointBalance;
    }
}
