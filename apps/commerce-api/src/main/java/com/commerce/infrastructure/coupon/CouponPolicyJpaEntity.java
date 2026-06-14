package com.commerce.infrastructure.coupon;

import java.time.ZonedDateTime;

import com.commerce.domain.coupon.CouponPolicy;
import com.commerce.infrastructure.jpa.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coupon_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponPolicyJpaEntity extends BaseJpaEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Embedded
    private DiscountRuleEmbeddable discountRule;

    @Column(name = "valid_days", nullable = false)
    private int validDays;

    @Column(name = "issuable_from", nullable = false)
    private ZonedDateTime issuableFrom;

    @Column(name = "issuable_until", nullable = false)
    private ZonedDateTime issuableUntil;

    @Column(name = "max_issue_count", nullable = false)
    private long maxIssueCount;

    @Column(name = "issued_count", nullable = false)
    private long issuedCount;

    @Column(name = "active", nullable = false)
    private boolean active;

    private CouponPolicyJpaEntity(String name, DiscountRuleEmbeddable discountRule, int validDays,
        ZonedDateTime issuableFrom, ZonedDateTime issuableUntil, long maxIssueCount, long issuedCount,
        boolean active) {
        this.name = name;
        this.discountRule = discountRule;
        this.validDays = validDays;
        this.issuableFrom = issuableFrom;
        this.issuableUntil = issuableUntil;
        this.maxIssueCount = maxIssueCount;
        this.issuedCount = issuedCount;
        this.active = active;
    }

    public static CouponPolicyJpaEntity fromDomain(CouponPolicy policy) {
        return new CouponPolicyJpaEntity(
            policy.getName(),
            DiscountRuleEmbeddable.fromDomain(policy.getDiscountRule()),
            policy.getValidDays(),
            policy.getIssuableFrom(),
            policy.getIssuableUntil(),
            policy.getMaxIssueCount(),
            policy.getIssuedCount(),
            policy.isActive()
        );
    }

    public CouponPolicy toDomain() {
        return CouponPolicy.reconstitute(
            getId(),
            name,
            discountRule.toDomain(),
            validDays,
            issuableFrom,
            issuableUntil,
            maxIssueCount,
            issuedCount,
            active
        );
    }

    public void updateFromDomain(CouponPolicy policy) {
        this.name = policy.getName();
        this.discountRule = DiscountRuleEmbeddable.fromDomain(policy.getDiscountRule());
        this.validDays = policy.getValidDays();
        this.issuableFrom = policy.getIssuableFrom();
        this.issuableUntil = policy.getIssuableUntil();
        this.maxIssueCount = policy.getMaxIssueCount();
        this.issuedCount = policy.getIssuedCount();
        this.active = policy.isActive();
    }
}
