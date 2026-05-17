package com.commerce.interfaces.api.member;

import com.commerce.application.member.MemberInfo;

public record SignUpResponse(
    Long id,
    String email,
    String nickname,
    String role
) {
    public static SignUpResponse from(MemberInfo info) {
        return new SignUpResponse(
            info.id(),
            info.email(),
            info.nickname(),
            info.role().name()
        );
    }
}
