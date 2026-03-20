package com.example.a3sproject.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class SignupUserRequest {

    @JsonProperty("name")
    @NotBlank(message = "이름은 필수입니다.")
    private String userName;

    @JsonProperty("email")
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식과 일치해야합니다.")
    private String userEmail;

    @JsonProperty("password")
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String userPassword;

    @JsonProperty("phone")
    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$", message = "전화번호 형식은 010-XXXX-XXXX 입니다.")
    private String userPhoneNumber;
}
