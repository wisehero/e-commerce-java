package com.commerce.infrastructure.coupon;

import com.commerce.domain.coupon.ApplicabilityScope;
import com.commerce.domain.coupon.ScopeType;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicabilityScopeEmbeddable {

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 20)
    private ScopeType type;

    @Column(name = "scope_target_id")
    private Long targetId;

    private ApplicabilityScopeEmbeddable(ScopeType type, Long targetId) {
        this.type = type;
        this.targetId = targetId;
    }

    public static ApplicabilityScopeEmbeddable fromDomain(ApplicabilityScope scope) {
        return new ApplicabilityScopeEmbeddable(scope.type(), scope.targetId());
    }

    public ApplicabilityScope toDomain() {
        return new ApplicabilityScope(type, targetId);
    }
}
