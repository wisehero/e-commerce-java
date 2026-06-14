package com.commerce.interfaces.api.coupon;

import java.time.ZonedDateTime;

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

    public CouponPolicyCreateCommand toCommand() {
        return new CouponPolicyCreateCommand(name, discountType, discountValue, maxDiscountAmount,
            minOrderAmount, validDays, issuableFrom, issuableUntil, maxIssueCount, active);
    }
}
