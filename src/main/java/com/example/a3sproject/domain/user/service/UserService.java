package com.example.a3sproject.domain.user.service;

import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.membership.repository.MembershipRepository;
import com.example.a3sproject.domain.user.dto.SignupUserRequest;
import com.example.a3sproject.domain.user.dto.SignupUserResponse;
import com.example.a3sproject.domain.user.dto.UserProfileResponseDto;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.UserException;
import com.example.a3sproject.global.security.JwtTokenProvider;
import com.example.a3sproject.global.security.refreshtoken.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupUserResponse createUser(SignupUserRequest request) {

        // 1. 이메일 중복 확인
        if (userRepository.existsByEmail(request.getUserEmail())) {
            throw new UserException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getUserPassword());

        // 3. User 저장
        User user = new User(
                request.getUserName(),
                request.getUserEmail(),
                encodedPassword,
                request.getUserPhoneNumber()
        );
        userRepository.save(user);

        // 4. Membership 초기화 및 저장
        Membership membership = Membership.init(user);
        membershipRepository.save(membership);

        // 5. 응답 반환
        return new SignupUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber()
        );
    }

    // 내 프로필 조회
    public UserProfileResponseDto getMyProfile(String email) {

        // 이메일로 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow( () -> new UserException(ErrorCode.USER_NOT_FOUND));

        // DTO 변환 후 반환
        return UserProfileResponseDto.from(user);
    }

}
