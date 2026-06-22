package com.commerce.application.coupon;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.brand.BrandRepository;
import com.commerce.domain.category.CategoryRepository;
import com.commerce.domain.coupon.ApplicabilityScope;
import com.commerce.domain.coupon.CouponPolicy;
import com.commerce.domain.coupon.CouponPolicyRepository;
import com.commerce.domain.coupon.DiscountRule;
import com.commerce.domain.coupon.DiscountType;
import com.commerce.domain.coupon.ScopeType;
import com.commerce.domain.member.MemberGrade;
import com.commerce.domain.product.ProductRepository;
import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponPolicyCreateUseCase {

    private final CouponPolicyRepository couponPolicyRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public CouponPolicyInfo create(CouponPolicyCreateCommand command) {
        DiscountRule rule = buildRule(command.discountType(), command.discountValue(),
            command.maxDiscountAmount(), command.minOrderAmount());
        ApplicabilityScope scope = buildScope(command.scopeType(), command.scopeTargetId());
        Map<MemberGrade, DiscountRule> gradeOverrides = buildGradeOverrides(command);

        CouponPolicy policy = CouponPolicy.create(
            command.name(),
            rule,
            scope,
            gradeOverrides,
            command.validDays(),
            command.issuableFrom(),
            command.issuableUntil(),
            command.maxIssueCount(),
            command.active()
        );
        return CouponPolicyInfo.from(couponPolicyRepository.save(policy));
    }

    private DiscountRule buildRule(DiscountType discountType, long value, Long maxDiscountAmount, long minOrderAmount) {
        return new DiscountRule(
            discountType,
            value,
            maxDiscountAmount == null ? null : new Money(maxDiscountAmount),
            new Money(minOrderAmount)
        );
    }

    /** 적용 범위를 만들고, 한정 범위면 대상 존재를 검증한다(죽은 쿠폰 차단). */
    private ApplicabilityScope buildScope(ScopeType type, Long targetId) {
        ApplicabilityScope scope = switch (type) {
            case WHOLE -> ApplicabilityScope.whole();
            case BRAND -> ApplicabilityScope.brand(targetId);
            case PRODUCT -> ApplicabilityScope.product(targetId);
            case CATEGORY -> ApplicabilityScope.category(targetId);
        };
        validateTargetExists(scope);
        return scope;
    }

    private void validateTargetExists(ApplicabilityScope scope) {
        switch (scope.type()) {
            case WHOLE -> {
            }
            case BRAND -> {
                if (brandRepository.findById(scope.targetId()).isEmpty()) {
                    throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다.");
                }
            }
            case PRODUCT -> {
                if (productRepository.findById(scope.targetId()).isEmpty()) {
                    throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.");
                }
            }
            case CATEGORY -> {
                if (categoryRepository.findById(scope.targetId()).isEmpty()) {
                    throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 카테고리입니다.");
                }
            }
        }
    }

    private Map<MemberGrade, DiscountRule> buildGradeOverrides(CouponPolicyCreateCommand command) {
        Map<MemberGrade, DiscountRule> overrides = new HashMap<>();
        if (command.gradeOverrides() == null) {
            return overrides;
        }
        for (CouponPolicyCreateCommand.GradeOverride override : command.gradeOverrides()) {
            overrides.put(override.grade(), buildRule(override.discountType(), override.discountValue(),
                override.maxDiscountAmount(), override.minOrderAmount()));
        }
        return overrides;
    }
}
