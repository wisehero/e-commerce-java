package com.commerce.interfaces.api.coupon;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commerce.application.coupon.CouponPolicyCreateUseCase;
import com.commerce.application.coupon.CouponPolicyInfo;
import com.commerce.interfaces.api.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/coupon-policies")
@RequiredArgsConstructor
public class CouponPolicyControllerV1 {

    private final CouponPolicyCreateUseCase couponPolicyCreateUseCase;

    @Operation(summary = "쿠폰 정책 생성")
    @PostMapping
    public ApiResponse<CouponPolicyResponse> create(@Valid @RequestBody CouponPolicyCreateRequest request) {
        CouponPolicyInfo info = couponPolicyCreateUseCase.create(request.toCommand());
        return ApiResponse.success(CouponPolicyResponse.from(info));
    }
}
