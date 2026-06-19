package com.commerce.interfaces.api.coupon;

import java.time.ZonedDateTime;
import java.util.List;

import com.commerce.application.coupon.CouponPolicyCreateCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CouponPolicyCreateRequest(
    @NotBlank(message = "쿠폰 정책명은 필수입니다.")
    String name,

    @NotBlank(message = "할인 유형은 필수입니다.")
    String discountType,

    @Positive(message = "할인 값은 1 이상이어야 합니다.")
    long discountValue,

    Long maxDiscountAmount,

    @PositiveOrZero(message = "최소 주문 금액은 0 이상이어야 합니다.")
    long minOrderAmount,

    // 적용 범위. null·빈 값이면 WHOLE(주문 전체)로 본다.
    String scopeType,

    // BRAND/PRODUCT/CATEGORY일 때 대상 ID. WHOLE이면 무시한다.
    Long scopeTargetId,

    // 등급별 할인 규칙 override. null이면 등급 차등 없음.
    List<GradeOverrideRequest> gradeOverrides,

    @Positive(message = "쿠폰 유효일수는 1일 이상이어야 합니다.")
    int validDays,

    @NotNull(message = "발급 시작 시각은 필수입니다.")
    ZonedDateTime issuableFrom,

    @NotNull(message = "발급 종료 시각은 필수입니다.")
    ZonedDateTime issuableUntil,

    @Positive(message = "최대 발급 수량은 1개 이상이어야 합니다.")
    long maxIssueCount,

    boolean active
) {

    public record GradeOverrideRequest(
        String grade,
        String discountType,
        long discountValue,
        Long maxDiscountAmount,
        long minOrderAmount
    ) {
    }

    public CouponPolicyCreateCommand toCommand() {
        List<CouponPolicyCreateCommand.GradeOverride> overrides = gradeOverrides == null
            ? List.of()
            : gradeOverrides.stream()
                .map(o -> new CouponPolicyCreateCommand.GradeOverride(
                    o.grade(), o.discountType(), o.discountValue(), o.maxDiscountAmount(), o.minOrderAmount()))
                .toList();
        return new CouponPolicyCreateCommand(name, discountType, discountValue, maxDiscountAmount,
            minOrderAmount, scopeType, scopeTargetId, overrides, validDays, issuableFrom, issuableUntil,
            maxIssueCount, active);
    }
}
