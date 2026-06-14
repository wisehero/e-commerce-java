package com.commerce.infrastructure.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponPolicyJpaRepository extends JpaRepository<CouponPolicyJpaEntity, Long> {

    @Modifying
    @Query("UPDATE CouponPolicyJpaEntity p SET p.issuedCount = p.issuedCount + 1 "
        + "WHERE p.id = :id AND p.issuedCount < p.maxIssueCount")
    int increaseIssuedCountIfAvailable(@Param("id") Long id);
}
