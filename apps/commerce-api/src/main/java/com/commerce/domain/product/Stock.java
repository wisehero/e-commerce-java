package com.commerce.domain.product;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

public record Stock(int quantity) {

    public Stock {
        if (quantity < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고는 0개 이상이어야 합니다.");
        }
    }

    public Stock decrease(int count) {
        validateCount(count);
        if (quantity < count) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }

        return new Stock(quantity - count);
    }

    public Stock increase(int count) {
        validateCount(count);
        return new Stock(quantity + count);
    }

    private static void validateCount(int count) {
        if (count <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 1개 이상이어야 합니다.");
        }
    }
}
