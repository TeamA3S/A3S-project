package com.example.a3sproject.domain.membership.dto;

import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.membership.enums.MembershipGrade;
import com.example.a3sproject.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyMembershipResponseDto {
    private final MembershipGrade grade;        // 등급명
    private final int totalPaymentAmount;        // 총 결제금액
    private final double earnRate;               // 적립률 (보너스 정보)

    public static MyMembershipResponseDto from(Membership membership, User user) {
        return MyMembershipResponseDto.builder()
                .grade(membership.getGrade())
                .totalPaymentAmount(user.getTotalPaymentAmount())
                .earnRate(membership.getEarnRate())
                .build();
    }
}