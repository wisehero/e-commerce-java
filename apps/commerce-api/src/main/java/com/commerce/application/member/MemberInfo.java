package com.commerce.application.member;

import com.commerce.domain.member.Member;
import com.commerce.domain.member.MemberRole;

public record MemberInfo(
    Long id,
    String email,
    String nickname,
    MemberRole role
) {
    public static MemberInfo from(Member member) {
        return new MemberInfo(
            member.getId(),
            member.getEmail().value(),
            member.getNickname(),
            member.getRole()
        );
    }
}
