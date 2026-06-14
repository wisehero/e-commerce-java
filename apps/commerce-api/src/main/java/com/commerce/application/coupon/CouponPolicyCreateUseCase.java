package com.commerce.application.coupon;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.coupon.CouponPolicy;
import com.commerce.domain.coupon.CouponPolicyRepository;
import com.commerce.domain.coupon.DiscountRule;
import com.commerce.domain.coupon.DiscountType;
import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponPolicyCreateUseCase {

    private final CouponPolicyRepository couponPolicyRepository;

    @Transactional
    public CouponPolicyInfo create(CouponPolicyCreateCommand command) {
        DiscountType discountType = parseDiscountType(command.discountType());
        DiscountRule rule = new DiscountRule(
            discountType,
            command.discountValue(),
            command.maxDiscountAmount() == null ? null : new Money(command.maxDiscountAmount()),
            new Money(command.minOrderAmount())
        );
        CouponPolicy policy = CouponPolicy.create(
            command.name(),
            rule,
            command.validDays(),
            command.issuableFrom(),
            command.issuableUntil(),
            command.maxIssueCount(),
            command.active()
        );
        return CouponPolicyInfo.from(couponPolicyRepository.save(policy));
    }

    private DiscountType parseDiscountType(String value) {
        try {
            return DiscountType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 할인 유형입니다: " + value);
        }
    }
}
