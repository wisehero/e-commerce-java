package com.commerce.domain.member;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.Getter;

@Getter
public class Member {

    private static final int NICKNAME_MIN = 2;
    private static final int NICKNAME_MAX = 20;

    private Long id;
    private Email email;
    private Password password;
    private String nickname;
    private MemberRole role;

    private Member(Long id, Email email, Password password, String nickname, MemberRole role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        validate();
    }

    public static Member register(Email email, Password password, String nickname) {
        return new Member(null, email, password, nickname, MemberRole.USER);
    }

    public static Member reconstitute(Long id, Email email, Password password, String nickname, MemberRole role) {
        return new Member(id, email, password, nickname, role);
    }

    private void validate() {
        if (email == null)
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 필수입니다.");
        if (password == null)
            throw new CoreException(ErrorType.BAD_REQUEST, "비밀번호는 필수입니다.");

        validateNickname(nickname);

        if (role == null)
            throw new CoreException(ErrorType.BAD_REQUEST, "권한은 필수입니다.");
    }

    private static void validateNickname(String nickname) {
        if (nickname == null || nickname.length() < NICKNAME_MIN || nickname.length() > NICKNAME_MAX) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("닉네임은 %d~%d자여야 합니다.", NICKNAME_MIN, NICKNAME_MAX));
        }
    }

    public boolean isAdmin() {
        return role == MemberRole.ADMIN;
    }

    public boolean hasRole(MemberRole target) {
        return this.role == target;
    }

    public boolean matchPassword(String rawPassword, PasswordHasher hasher) {
        return password.matches(rawPassword, hasher);
    }

    public void changeNickname(String newNickname) {
        validateNickname(newNickname);
        this.nickname = newNickname;
    }

}
