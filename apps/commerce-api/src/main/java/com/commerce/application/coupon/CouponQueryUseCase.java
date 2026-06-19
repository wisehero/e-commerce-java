package com.commerce.application.coupon;

import java.time.ZonedDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.coupon.CouponStatus;
import com.commerce.domain.coupon.IssuedCoupon;
import com.commerce.domain.coupon.IssuedCouponRepository;
import com.commerce.support.page.PageQuery;
import com.commerce.support.page.PageResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponQueryUseCase {

    private final IssuedCouponRepository issuedCouponRepository;

    @Transactional(readOnly = true)
    public PageResult<CouponInfo> getByMember(Long memberId, String status, int page, int size) {
        ZonedDateTime now = ZonedDateTime.now();
        PageQuery pageQuery = new PageQuery(page, size);
        PageResult<IssuedCoupon> result = issuedCouponRepository.findByMemberId(
            memberId, parseStatus(status), pageQuery.page(), pageQuery.size());
        return new PageResult<>(
            result.items().stream().map(coupon -> CouponInfo.from(coupon, now)).toList(),
            result.totalCount(),
            result.page(),
            result.size()
        );
    }

    private CouponStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return CouponStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new com.commerce.support.error.CoreException(
                com.commerce.support.error.ErrorType.BAD_REQUEST, "지원하지 않는 쿠폰 상태입니다: " + value);
        }
    }
}
