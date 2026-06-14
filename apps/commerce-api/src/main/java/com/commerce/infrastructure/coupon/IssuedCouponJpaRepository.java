package com.commerce.infrastructure.coupon;

import java.time.ZonedDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.commerce.domain.coupon.CouponStatus;

public interface IssuedCouponJpaRepository extends JpaRepository<IssuedCouponJpaEntity, Long> {

    Page<IssuedCouponJpaEntity> findByMemberId(Long memberId, Pageable pageable);

    Page<IssuedCouponJpaEntity> findByMemberIdAndStatus(Long memberId, CouponStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE IssuedCouponJpaEntity c SET c.status = :usedStatus, c.usedOrderId = :orderId "
        + "WHERE c.id = :id AND c.memberId = :memberId AND c.status = :unusedStatus AND c.expiresAt > :now")
    int markUsedIfAvailable(@Param("id") Long id, @Param("memberId") Long memberId,
        @Param("now") ZonedDateTime now, @Param("orderId") Long orderId,
        @Param("unusedStatus") CouponStatus unusedStatus, @Param("usedStatus") CouponStatus usedStatus);

    @Modifying
    @Query("UPDATE IssuedCouponJpaEntity c SET c.status = :unusedStatus, c.usedOrderId = NULL "
        + "WHERE c.status = :usedStatus AND c.usedOrderId = :orderId")
    int restoreByOrderId(@Param("orderId") Long orderId,
        @Param("usedStatus") CouponStatus usedStatus, @Param("unusedStatus") CouponStatus unusedStatus);
}
