package com.commerce.interfaces.api.coupon;

import com.commerce.application.coupon.CouponIssueCommand;

import jakarta.validation.constraints.NotNull;

public record CouponIssueRequest(
    @NotNull(message = "쿠폰 정책 ID는 필수입니다.")
    Long policyId
) {

    public CouponIssueCommand toCommand(Long memberId) {
        return new CouponIssueCommand(policyId, memberId);
    }
}
