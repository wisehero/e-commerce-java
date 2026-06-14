package com.commerce.infrastructure.coupon;

import java.time.ZonedDateTime;

import com.commerce.domain.coupon.CouponStatus;
import com.commerce.domain.coupon.IssuedCoupon;
import com.commerce.infrastructure.jpa.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "issued_coupons",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_issued_coupons_policy_member", columnNames = {"policy_id", "member_id"})
    },
    indexes = {
        @Index(name = "idx_issued_coupons_member_status", columnList = "member_id,status"),
        @Index(name = "idx_issued_coupons_used_order", columnList = "used_order_id")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssuedCouponJpaEntity extends BaseJpaEntity {

    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Embedded
    private DiscountRuleEmbeddable discountRule;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CouponStatus status;

    @Column(name = "issued_at", nullable = false)
    private ZonedDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private ZonedDateTime expiresAt;

    @Column(name = "used_order_id")
    private Long usedOrderId;

    private IssuedCouponJpaEntity(Long policyId, Long memberId, DiscountRuleEmbeddable discountRule,
        CouponStatus status, ZonedDateTime issuedAt, ZonedDateTime expiresAt, Long usedOrderId) {
        this.policyId = policyId;
        this.memberId = memberId;
        this.discountRule = discountRule;
        this.status = status;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.usedOrderId = usedOrderId;
    }

    public static IssuedCouponJpaEntity fromDomain(IssuedCoupon issuedCoupon) {
        return new IssuedCouponJpaEntity(
            issuedCoupon.getPolicyId(),
            issuedCoupon.getMemberId(),
            DiscountRuleEmbeddable.fromDomain(issuedCoupon.getDiscountRule()),
            issuedCoupon.getStatus(),
            issuedCoupon.getIssuedAt(),
            issuedCoupon.getExpiresAt(),
            issuedCoupon.getUsedOrderId()
        );
    }

    public IssuedCoupon toDomain() {
        return IssuedCoupon.reconstitute(
            getId(),
            policyId,
            memberId,
            discountRule.toDomain(),
            status,
            issuedAt,
            expiresAt,
            usedOrderId
        );
    }

    public void updateFromDomain(IssuedCoupon issuedCoupon) {
        this.status = issuedCoupon.getStatus();
        this.usedOrderId = issuedCoupon.getUsedOrderId();
    }
}
