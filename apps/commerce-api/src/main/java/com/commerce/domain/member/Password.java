package com.commerce.domain.member;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

public record Password(String hashedValue) {

    private static final int MIN_RAW_LENGTH = 8;

    public Password {
        if (hashedValue == null || hashedValue.isBlank()) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "잘못된 비밀번호 해시값");
        }
    }

    /** 평문을 받아 해싱해 신규 생성 */
    public static Password of(String rawPassword, PasswordHasher hasher) {
        validateRaw(rawPassword);
        return new Password(hasher.hash(rawPassword));
    }

    /** DB에서 읽은 해시값으로 재구성 */
    public static Password ofHashed(String hashedValue) {
        return new Password(hashedValue);
    }

    public boolean matches(String rawPassword, PasswordHasher hasher) {
        return hasher.matches(rawPassword, this.hashedValue);
    }

    @Override
    public String toString() {
        return "Password[***]";  // 해시값 로그 누출 방지
    }

    private static void validateRaw(String raw) {
        if (raw == null || raw.length() < MIN_RAW_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("비밀번호는 %d자 이상이어야 합니다.", MIN_RAW_LENGTH));
        }
    }
}
