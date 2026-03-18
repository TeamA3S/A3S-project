package com.example.a3sproject.domain.user.dto;

import com.example.a3sproject.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileResponseDto {
    private final String name;
    private final String email;
    private final String phoneNumber;
    private final Integer pointBalance;

    public static UserProfileResponseDto from(User user) {
        return UserProfileResponseDto.builder()
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .pointBalance(user.getPointBalance())
                .build();
    }
}
