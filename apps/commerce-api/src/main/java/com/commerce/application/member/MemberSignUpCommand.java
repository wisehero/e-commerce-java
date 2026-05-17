package com.commerce.application.member;

public record MemberSignUpCommand(
    String email,
    String rawPassword,
    String nickname
) {
}
