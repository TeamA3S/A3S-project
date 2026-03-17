package com.example.a3sproject.domain.membership.entity;

import com.example.a3sproject.domain.membership.enums.MembershipGrade;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.global.entity.BaseEntity;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.PointException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "memberships")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Membership extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User와 1:1 관계
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // 현재 멤버십 등급
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipGrade grade;

    // 회원가입시 초기화용 정적 팩토리 메서드
    public static  Membership init(User user) {
        Membership membership = new Membership();
        membership.user = user;
        membership.grade = MembershipGrade.NORMAL;
        return membership;
    }

    // User의 totalPaymentAmount 기반으로 등급 갱신
    public void updateGrade(int totalPaymentAmount) {
        this.grade = calculateGrade(totalPaymentAmount);
    }

    // 등급 갱신 로직
    private MembershipGrade calculateGrade(int totalAmount) {
        if (totalAmount >= 500_000) return MembershipGrade.VVIP;
        if (totalAmount >= 300_000) return MembershipGrade.VIP;
        return MembershipGrade.NORMAL;
    }

    // 등급별 적립률
    public double getEarnRate() {
        return switch (this.grade) {
            case VVIP -> 0.10;
            case VIP -> 0.05;
            default -> 0.01;
        };
    }

}
