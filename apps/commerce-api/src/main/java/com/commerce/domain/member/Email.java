package com.commerce.domain.member;

import java.util.regex.Pattern;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

public record Email(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 필수입니다.");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.");
        }
    }
}
