package com.commerce.infrastructure.coupon;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.commerce.domain.coupon.CouponPolicy;
import com.commerce.domain.coupon.CouponPolicyRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CouponPolicyRepositoryImpl implements CouponPolicyRepository {

    private final CouponPolicyJpaRepository couponPolicyJpaRepository;

    @Override
    public CouponPolicy save(CouponPolicy policy) {
        if (policy.getId() == null) {
            return couponPolicyJpaRepository.save(CouponPolicyJpaEntity.fromDomain(policy)).toDomain();
        }
        CouponPolicyJpaEntity entity = couponPolicyJpaRepository.findById(policy.getId())
            .orElseThrow(() -> new IllegalStateException("Coupon policy not found: " + policy.getId()));
        entity.updateFromDomain(policy);
        return entity.toDomain();
    }

    @Override
    public Optional<CouponPolicy> findById(Long id) {
        return couponPolicyJpaRepository.findById(id)
            .map(CouponPolicyJpaEntity::toDomain);
    }

    @Override
    public boolean increaseIssuedCountIfAvailable(Long id) {
        return couponPolicyJpaRepository.increaseIssuedCountIfAvailable(id) == 1;
    }
}
