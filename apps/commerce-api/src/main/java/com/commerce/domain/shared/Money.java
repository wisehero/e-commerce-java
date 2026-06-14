package com.commerce.domain.shared;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

public record Money(long amount) {

    public static final Money ZERO = new Money(0);

    public Money {
        if (amount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "금액은 0원 이상이어야 합니다.");
        }
    }

    /** salePrice ≤ originalPrice 같은 도메인 invariant 검증용 */
    public boolean isGreaterThan(Money other) {
        return this.amount > other.amount;
    }

    /** 주문 총액 합산용 (라인 금액의 reduce 시작점은 ZERO) */
    public Money plus(Money other) {
        return new Money(this.amount + other.amount);
    }

    public Money minus(Money other) {
        return new Money(this.amount - other.amount);
    }

    public Money min(Money other) {
        return this.amount <= other.amount ? this : other;
    }

    public boolean isLessThan(Money other) {
        return this.amount < other.amount;
    }

    public boolean isZero() {
        return amount == 0;
    }

    /** 주문 라인 금액 = 단가 × 수량 */
    public Money multiply(int quantity) {
        return new Money(this.amount * quantity);
    }
}
