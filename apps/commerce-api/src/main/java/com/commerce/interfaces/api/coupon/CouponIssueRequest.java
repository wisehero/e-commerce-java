package com.commerce.interfaces.api.coupon;

import com.commerce.application.coupon.CouponIssueCommand;

import jakarta.validation.constraints.NotNull;

public record CouponIssueRequest(
    @NotNull(message = "쿠폰 정책 ID는 필수입니다.")
    Long policyId,

    @NotNull(message = "회원 ID는 필수입니다.")
    Long memberId
) {

    public CouponIssueCommand toCommand() {
        return new CouponIssueCommand(policyId, memberId);
    }
}
