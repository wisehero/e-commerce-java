package com.commerce.infrastructure.coupon;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import com.commerce.domain.coupon.CouponPolicy;
import com.commerce.domain.coupon.DiscountRule;
import com.commerce.domain.member.MemberGrade;
import com.commerce.infrastructure.jpa.BaseJpaEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
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

    @Embedded
    private ApplicabilityScopeEmbeddable applicabilityScope;

    /**
     * 등급별 할인 규칙 override. 부분 맵이며 비어 있으면 비차등 쿠폰이다.
     * Aggregate 내부 value 컬렉션이라 @ElementCollection으로 매핑한다(연관 어노테이션 없음, 기본 FK 컬럼).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "coupon_policy_grade_overrides")
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "grade", length = 20)
    private Map<MemberGrade, DiscountRuleEmbeddable> gradeOverrides = new HashMap<>();

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

    private CouponPolicyJpaEntity(String name, DiscountRuleEmbeddable discountRule,
        ApplicabilityScopeEmbeddable applicabilityScope, Map<MemberGrade, DiscountRuleEmbeddable> gradeOverrides,
        int validDays, ZonedDateTime issuableFrom, ZonedDateTime issuableUntil, long maxIssueCount,
        long issuedCount, boolean active) {
        this.name = name;
        this.discountRule = discountRule;
        this.applicabilityScope = applicabilityScope;
        this.gradeOverrides = gradeOverrides;
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
            ApplicabilityScopeEmbeddable.fromDomain(policy.getApplicabilityScope()),
            toEmbeddableOverrides(policy.getGradeOverrides()),
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
            applicabilityScope.toDomain(),
            toDomainOverrides(gradeOverrides),
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
        this.applicabilityScope = ApplicabilityScopeEmbeddable.fromDomain(policy.getApplicabilityScope());
        this.gradeOverrides = toEmbeddableOverrides(policy.getGradeOverrides());
        this.validDays = policy.getValidDays();
        this.issuableFrom = policy.getIssuableFrom();
        this.issuableUntil = policy.getIssuableUntil();
        this.maxIssueCount = policy.getMaxIssueCount();
        this.issuedCount = policy.getIssuedCount();
        this.active = policy.isActive();
    }

    private static Map<MemberGrade, DiscountRuleEmbeddable> toEmbeddableOverrides(
        Map<MemberGrade, DiscountRule> overrides) {
        Map<MemberGrade, DiscountRuleEmbeddable> result = new HashMap<>();
        overrides.forEach((grade, rule) -> result.put(grade, DiscountRuleEmbeddable.fromDomain(rule)));
        return result;
    }

    private static Map<MemberGrade, DiscountRule> toDomainOverrides(
        Map<MemberGrade, DiscountRuleEmbeddable> overrides) {
        Map<MemberGrade, DiscountRule> result = new HashMap<>();
        overrides.forEach((grade, embeddable) -> result.put(grade, embeddable.toDomain()));
        return result;
    }
}
