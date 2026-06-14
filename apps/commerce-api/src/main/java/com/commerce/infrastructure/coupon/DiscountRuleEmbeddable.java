package com.commerce.infrastructure.coupon;

import com.commerce.domain.coupon.DiscountRule;
import com.commerce.domain.coupon.DiscountType;
import com.commerce.domain.shared.Money;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiscountRuleEmbeddable {

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType type;

    @Column(name = "discount_value", nullable = false)
    private long value;

    @Column(name = "max_discount_amount")
    private Long maxDiscountAmount;

    @Column(name = "min_order_amount", nullable = false)
    private long minOrderAmount;

    private DiscountRuleEmbeddable(DiscountType type, long value, Long maxDiscountAmount, long minOrderAmount) {
        this.type = type;
        this.value = value;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minOrderAmount = minOrderAmount;
    }

    public static DiscountRuleEmbeddable fromDomain(DiscountRule rule) {
        return new DiscountRuleEmbeddable(
            rule.type(),
            rule.value(),
            rule.maxDiscountAmount() == null ? null : rule.maxDiscountAmount().amount(),
            rule.minOrderAmount().amount()
        );
    }

    public DiscountRule toDomain() {
        return new DiscountRule(
            type,
            value,
            maxDiscountAmount == null ? null : new Money(maxDiscountAmount),
            new Money(minOrderAmount)
        );
    }
}
