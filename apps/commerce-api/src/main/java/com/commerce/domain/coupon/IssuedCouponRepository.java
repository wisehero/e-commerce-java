package com.commerce.domain.coupon;

import java.time.ZonedDateTime;
import java.util.Optional;

import com.commerce.support.page.PageResult;

public interface IssuedCouponRepository {

    IssuedCoupon save(IssuedCoupon issuedCoupon);

    Optional<IssuedCoupon> findById(Long id);

    PageResult<IssuedCoupon> findByMemberId(Long memberId, CouponStatus status, int page, int size);

    boolean markUsedIfAvailable(Long id, Long memberId, ZonedDateTime now, Long orderId);

    void restoreByOrderId(Long orderId);
}
