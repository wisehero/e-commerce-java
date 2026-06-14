package com.commerce.domain.coupon;

import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

public record DiscountRule(
    DiscountType type,
    long value,
    Money maxDiscountAmount,
    Money minOrderAmount
) {

    public DiscountRule {
        if (type == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 유형은 필수입니다.");
        }
        if (type == DiscountType.FIXED && value <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "정액 할인 금액은 0원보다 커야 합니다.");
        }
        if (type == DiscountType.RATE && (value < 1 || value > 100)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "정률 할인율은 1% 이상 100% 이하여야 합니다.");
        }
        if (minOrderAmount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "최소 주문 금액은 필수입니다.");
        }
    }

    public Money calculateDiscount(Money orderAmount) {
        if (orderAmount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 금액은 필수입니다.");
        }
        if (orderAmount.isLessThan(minOrderAmount)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 적용 최소 주문 금액을 충족하지 못했습니다.");
        }

        Money raw = switch (type) {
            case FIXED -> new Money(value);
            case RATE -> new Money(orderAmount.amount() * value / 100);
        };
        Money capped = maxDiscountAmount == null ? raw : raw.min(maxDiscountAmount);
        return capped.min(orderAmount);
    }
}
