package com.commerce.domain.member;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.Getter;

@Getter
public class Member {

    private static final int NICKNAME_MIN = 2;
    private static final int NICKNAME_MAX = 20;
    private static final int LOGIN_FAILURE_LOCK_THRESHOLD = 5;
    private static final int INITIAL_LOGIN_FAILURE_COUNT = 0;
    private static final int INITIAL_AUTH_VERSION = 1;

    private Long id;
    private Email email;
    private Password password;
    private String nickname;
    private MemberRole role;
    private MemberGrade grade;
    private MemberStatus status;
    private int loginFailureCount;
    private int authVersion;

    private Member(Long id, Email email, Password password, String nickname, MemberRole role, MemberGrade grade,
        MemberStatus status, int loginFailureCount, int authVersion) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.grade = grade;
        this.status = status;
        this.loginFailureCount = loginFailureCount;
        this.authVersion = authVersion;
        validate();
    }

    public static Member register(Email email, Password password, String nickname) {
        return new Member(null, email, password, nickname, MemberRole.USER, MemberGrade.BRONZE, MemberStatus.ACTIVE,
            INITIAL_LOGIN_FAILURE_COUNT, INITIAL_AUTH_VERSION);
    }

    public static Member reconstitute(Long id, Email email, Password password, String nickname, MemberRole role,
        MemberGrade grade) {
        return reconstitute(id, email, password, nickname, role, grade, MemberStatus.ACTIVE,
            INITIAL_LOGIN_FAILURE_COUNT, INITIAL_AUTH_VERSION);
    }

    public static Member reconstitute(Long id, Email email, Password password, String nickname, MemberRole role,
        MemberGrade grade, MemberStatus status, int loginFailureCount, int authVersion) {
        return new Member(id, email, password, nickname, role, grade, status, loginFailureCount, authVersion);
    }

    private void validate() {
        if (email == null)
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 필수입니다.");
        if (password == null)
            throw new CoreException(ErrorType.BAD_REQUEST, "비밀번호는 필수입니다.");

        validateNickname(nickname);

        if (role == null)
            throw new CoreException(ErrorType.BAD_REQUEST, "권한은 필수입니다.");
        if (grade == null)
            throw new CoreException(ErrorType.BAD_REQUEST, "등급은 필수입니다.");
        if (status == null)
            throw new CoreException(ErrorType.BAD_REQUEST, "회원 상태는 필수입니다.");
        if (loginFailureCount < 0)
            throw new CoreException(ErrorType.BAD_REQUEST, "로그인 실패 횟수는 0 이상이어야 합니다.");
        if (authVersion < INITIAL_AUTH_VERSION)
            throw new CoreException(ErrorType.BAD_REQUEST, "인증 버전은 1 이상이어야 합니다.");
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

    public void recordLoginFailure() {
        ensureLoginAllowed();
        this.loginFailureCount++;
        if (this.loginFailureCount >= LOGIN_FAILURE_LOCK_THRESHOLD) {
            lockByLoginFailures();
        }
    }

    public void resetLoginFailures() {
        this.loginFailureCount = 0;
    }

    public void ensureLoginAllowed() {
        if (!status.canLogin()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "로그인할 수 없는 회원 상태입니다.");
        }
    }

    public void suspend() {
        changeStatus(MemberStatus.SUSPENDED);
    }

    public void withdraw() {
        changeStatus(MemberStatus.WITHDRAWN);
    }

    public void unlock() {
        if (this.status == MemberStatus.WITHDRAWN) {
            throw new CoreException(ErrorType.BAD_REQUEST, "탈퇴한 회원은 잠금 해제할 수 없습니다.");
        }
        changeStatus(MemberStatus.ACTIVE);
        resetLoginFailures();
    }

    public void increaseAuthVersion() {
        this.authVersion++;
    }

    public void changeNickname(String newNickname) {
        validateNickname(newNickname);
        this.nickname = newNickname;
    }

    /** 운영자가 회원 등급을 변경한다. 자동 산정이 아니라 수동 지정이다. */
    public void changeGrade(MemberGrade newGrade) {
        if (newGrade == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "변경할 등급은 필수입니다.");
        }
        this.grade = newGrade;
    }

    private void lockByLoginFailures() {
        changeStatus(MemberStatus.LOCKED);
    }

    private void changeStatus(MemberStatus newStatus) {
        if (newStatus == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "변경할 회원 상태는 필수입니다.");
        }
        if (this.status == newStatus) {
            return;
        }
        this.status = newStatus;
        increaseAuthVersion();
    }

}
