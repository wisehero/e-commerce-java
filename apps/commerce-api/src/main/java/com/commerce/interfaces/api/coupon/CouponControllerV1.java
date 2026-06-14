package com.commerce.interfaces.api.coupon;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.commerce.application.coupon.CouponInfo;
import com.commerce.application.coupon.CouponIssueUseCase;
import com.commerce.application.coupon.CouponQueryUseCase;
import com.commerce.interfaces.api.ApiResponse;
import com.commerce.interfaces.api.PageResponse;
import com.commerce.support.page.PageResult;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CouponControllerV1 {

    private final CouponIssueUseCase couponIssueUseCase;
    private final CouponQueryUseCase couponQueryUseCase;

    @Operation(summary = "쿠폰 발급")
    @PostMapping("/api/v1/coupons")
    public ApiResponse<CouponResponse> issue(@Valid @RequestBody CouponIssueRequest request) {
        return ApiResponse.success(CouponResponse.from(couponIssueUseCase.issue(request.toCommand())));
    }

    @Operation(summary = "회원 쿠폰 목록 조회")
    @GetMapping("/api/v1/members/{memberId}/coupons")
    public ApiResponse<PageResponse<CouponResponse>> getByMember(
        @PathVariable Long memberId,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        PageResult<CouponInfo> result = couponQueryUseCase.getByMember(memberId, status, page, size);
        return ApiResponse.success(PageResponse.of(result, CouponResponse::from));
    }
}
