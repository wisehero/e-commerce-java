package com.commerce.domain.product;

import java.util.List;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

public record ProductSearchCondition(
    String keyword, List<Long> categoryIds, Long brandId, int page, int size
) {

    private static final int MAX_SIZE = 100;

    public ProductSearchCondition {
        if (page < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "페이지 번호는 0 이상이어야 합니다.");
        }
        if (size <= 0 || size > MAX_SIZE) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("페이지 크기는 1~%d 사이여야 합니다.", MAX_SIZE));
        }
    }

}
