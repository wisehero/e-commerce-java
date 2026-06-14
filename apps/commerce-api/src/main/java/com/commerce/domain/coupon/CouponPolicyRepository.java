package com.commerce.domain.coupon;

import java.util.Optional;

public interface CouponPolicyRepository {

    CouponPolicy save(CouponPolicy policy);

    Optional<CouponPolicy> findById(Long id);

    boolean increaseIssuedCountIfAvailable(Long id);
}
