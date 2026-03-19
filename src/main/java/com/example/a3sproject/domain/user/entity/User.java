package com.example.a3sproject.domain.user.entity;

import com.example.a3sproject.domain.membership.enums.MembershipGrade;
import com.example.a3sproject.global.entity.BaseEntity;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.PointException;
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

    // 총 결제 금액
    @Column(nullable = false)
    private Integer totalPaymentAmount;

    @Column(nullable = false)
    private MembershipGrade membershipGrade;

    // 회원가입용 생성자
    public User(String name, String email, String password, String phoneNumber) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.pointBalance = 0;
        this.totalPaymentAmount = 0;
        this.membershipGrade = MembershipGrade.NORMAL;
    }

    // 총 결제 금액 업데이트
    public void updateTotalPaymentAmount(int amount) {
        this.totalPaymentAmount += amount;
    }

    // 포인트 차감
    public void usePoint(int amount) {
        if (this.pointBalance < amount) {
            throw new PointException(ErrorCode.POINT_NOT_ENOUGH);
        }
        this.pointBalance -= amount;
    }

    // 포인트 적립
    public void earnPoint(int amount) {
        this.pointBalance += amount;
    }

    // 포인트 복구
    public void restorePoint(int amount) {
        this.pointBalance += amount;
    }
}
