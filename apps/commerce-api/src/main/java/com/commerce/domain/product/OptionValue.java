package com.commerce.domain.product;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

/**
 *
 * @param name 옵션 축 ex) 색상, 사이즈
 * @param value 옵션의 값 ex) 빨강, "L"
 */
public record OptionValue(String name, String value) {

    public OptionValue {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "옵션명은 필수입니다.");
        }

        if (value == null || value.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "옵션값은 필수입니다.");
        }
    }
}
