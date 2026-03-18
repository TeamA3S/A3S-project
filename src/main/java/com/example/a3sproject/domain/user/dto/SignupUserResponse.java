package com.example.a3sproject.domain.user.dto;

import lombok.Getter;

@Getter
public class SignupUserResponse {
    private final Long userId;
    private final String userName;
    private final String userEmail;
    private final String userPhoneNumber;

    public SignupUserResponse(Long userId, String userName, String userEmail, String userPhoneNumber) {
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.userPhoneNumber = userPhoneNumber;
    }
}
