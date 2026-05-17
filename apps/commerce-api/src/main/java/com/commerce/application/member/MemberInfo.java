package com.commerce.application.member;

import com.commerce.domain.member.Member;

public record MemberInfo(
    Long id,
    String email,
    String nickname,
    String role
) {
    public static MemberInfo from(Member member) {
        return new MemberInfo(
            member.getId(),
            member.getEmail().value(),
            member.getNickname(),
            member.getRole().name()
        );
    }
}
