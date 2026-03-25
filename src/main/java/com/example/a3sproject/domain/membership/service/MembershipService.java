package com.example.a3sproject.domain.membership.service;

import com.example.a3sproject.domain.membership.dto.MyMembershipResponseDto;
import com.example.a3sproject.domain.membership.entity.Membership;
import com.example.a3sproject.domain.membership.repository.MembershipRepository;
import com.example.a3sproject.domain.user.entity.User;
import com.example.a3sproject.domain.user.repository.UserRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    // 내 멤버십 등급 조회
    public MyMembershipResponseDto getMyMembership(User user) {

        // 2. 유저로 멤버십 조회
        Membership membership = membershipRepository.findByUser(user)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 3. DTO 변환 후 반환
        return MyMembershipResponseDto.from(membership, user);
    }
}