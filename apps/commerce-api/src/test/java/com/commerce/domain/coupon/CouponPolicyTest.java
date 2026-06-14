package com.commerce.domain.coupon;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

class CouponPolicyTest {

    private static final ZonedDateTime NOW = ZonedDateTime.parse("2026-06-14T10:00:00+09:00[Asia/Seoul]");

    private CouponPolicy policy(boolean active, long issuedCount, long maxIssueCount) {
        return CouponPolicy.reconstitute(1L, "선착순 쿠폰",
            new DiscountRule(DiscountType.FIXED, 1000L, null, Money.ZERO),
            7, NOW.minusDays(1), NOW.plusDays(1), maxIssueCount, issuedCount, active);
    }

    @Nested
    @DisplayName("assertIssuable")
    class AssertIssuable {

        @Test
        @DisplayName("활성 상태이고 기간 안이며 수량이 남아 있으면 예외가 발생하지 않는다")
        void should_notThrow_when_issuable() {
            policy(true, 0L, 10L).assertIssuable(NOW);
        }

        @Test
        @DisplayName("비활성 정책이면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_inactive() {
            assertThatThrownBy(() -> policy(false, 0L, 10L).assertIssuable(NOW))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("발급 수량이 소진되면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_soldOut() {
            assertThatThrownBy(() -> policy(true, 10L, 10L).assertIssuable(NOW))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
