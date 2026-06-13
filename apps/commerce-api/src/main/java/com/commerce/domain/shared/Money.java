package com.commerce.domain.shared;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

public record Money(long amount) {

    public Money {
        if (amount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "금액은 0원 이상이어야 합니다.");
        }
    }

    /** salePrice ≤ originalPrice 같은 도메인 invariant 검증용 */
    public boolean isGreaterThan(Money other) {
        return this.amount > other.amount;
    }
}
