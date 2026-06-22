package com.commerce.infrastructure.coupon;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.commerce.domain.coupon.ApplicabilityScope;
import com.commerce.domain.coupon.CouponPolicy;
import com.commerce.domain.coupon.CouponStatus;
import com.commerce.domain.coupon.DiscountRule;
import com.commerce.domain.coupon.DiscountType;
import com.commerce.domain.coupon.IssuedCoupon;
import com.commerce.domain.coupon.IssuedCouponRepository;
import com.commerce.domain.member.MemberGrade;
import com.commerce.domain.shared.Money;
import com.commerce.support.IntegrationTestSupport;

/**
 * IssuedCoupon 사용/복원의 조건부 원자 UPDATE가 도메인 use()/restore()와 같은 규칙으로
 * 동작하는지 실 DB로 고정한다. coupon-domain-spec §10 "두 경로가 드리프트하지 않게
 * 통합 테스트로 고정한다" 약속의 사용/복원 쪽 이행분이다(발급 quota는 CouponIssueConcurrencyIntegrationTest).
 *
 * <p>markUsedIfAvailable·restoreByOrderId는 @Modifying UPDATE라 트랜잭션이 필요하다.
 * 프로덕션에서 OrderPlaceUseCase의 Txn1 안에서 호출되는 것과 동일하게 TransactionTemplate로
 * 감싸고, 검증 조회는 그 트랜잭션 밖에서 수행해 1차 캐시 stale을 피한다.
 */
class IssuedCouponUpdateIntegrationTest extends IntegrationTestSupport {

    private static final Long MEMBER_ID = 1L;
    private static final Long POLICY_ID = 10L;
    private static final Long ORDER_ID = 1000L;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @Autowired
    private IssuedCouponJpaRepository issuedCouponJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void tearDown() {
        issuedCouponJpaRepository.deleteAll();
    }

    private IssuedCoupon savedUnusedCoupon(ZonedDateTime now) {
        CouponPolicy policy = CouponPolicy.reconstitute(POLICY_ID, "테스트 쿠폰",
            new DiscountRule(DiscountType.FIXED, 1000L, null, Money.ZERO),
            ApplicabilityScope.whole(), Map.of(), 7, now.minusDays(1), now.plusDays(1), 10L, 0L, true);
        return issuedCouponRepository.save(IssuedCoupon.issue(policy, MEMBER_ID, MemberGrade.BRONZE, now));
    }

    private CouponStatus statusOf(Long id) {
        return issuedCouponRepository.findById(id).orElseThrow().getStatus();
    }

    @Nested
    @DisplayName("markUsedIfAvailable — use()와 동일한 가드")
    class MarkUsed {

        @Test
        @DisplayName("미사용·미만료·소유자 일치면 USED로 전이하고 true를 반환한다")
        void should_markUsed_when_available() {
            ZonedDateTime now = ZonedDateTime.now();
            IssuedCoupon coupon = savedUnusedCoupon(now);

            boolean result = tx.execute(s ->
                issuedCouponRepository.markUsedIfAvailable(coupon.getId(), MEMBER_ID, now, ORDER_ID));

            assertThat(result).isTrue();
            IssuedCoupon reloaded = issuedCouponRepository.findById(coupon.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(CouponStatus.USED);
            assertThat(reloaded.getUsedOrderId()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("소유자가 다르면 전이하지 않고 false를 반환한다")
        void should_notMark_when_ownerMismatch() {
            ZonedDateTime now = ZonedDateTime.now();
            IssuedCoupon coupon = savedUnusedCoupon(now);

            boolean result = tx.execute(s ->
                issuedCouponRepository.markUsedIfAvailable(coupon.getId(), 999L, now, ORDER_ID));

            assertThat(result).isFalse();
            assertThat(statusOf(coupon.getId())).isEqualTo(CouponStatus.UNUSED);
        }

        @Test
        @DisplayName("만료 시각 이후면 전이하지 않고 false를 반환한다")
        void should_notMark_when_expired() {
            ZonedDateTime now = ZonedDateTime.now();
            IssuedCoupon coupon = savedUnusedCoupon(now);
            ZonedDateTime afterExpiry = coupon.getExpiresAt().plusSeconds(1);

            boolean result = tx.execute(s ->
                issuedCouponRepository.markUsedIfAvailable(coupon.getId(), MEMBER_ID, afterExpiry, ORDER_ID));

            assertThat(result).isFalse();
            assertThat(statusOf(coupon.getId())).isEqualTo(CouponStatus.UNUSED);
        }

        @Test
        @DisplayName("이미 사용된 쿠폰이면 false를 반환하고 첫 주문을 유지한다")
        void should_notMark_when_alreadyUsed() {
            ZonedDateTime now = ZonedDateTime.now();
            IssuedCoupon coupon = savedUnusedCoupon(now);
            tx.execute(s -> issuedCouponRepository.markUsedIfAvailable(coupon.getId(), MEMBER_ID, now, ORDER_ID));

            boolean second = tx.execute(s ->
                issuedCouponRepository.markUsedIfAvailable(coupon.getId(), MEMBER_ID, now, 2000L));

            assertThat(second).isFalse();
            assertThat(issuedCouponRepository.findById(coupon.getId()).orElseThrow().getUsedOrderId())
                .isEqualTo(ORDER_ID);
        }
    }

    @Nested
    @DisplayName("restoreByOrderId — restore()와 동일한 가드(멱등)")
    class Restore {

        @Test
        @DisplayName("해당 주문으로 사용된 쿠폰을 UNUSED로 되돌리고 주문 ID를 제거한다")
        void should_restore_when_usedByOrder() {
            ZonedDateTime now = ZonedDateTime.now();
            IssuedCoupon coupon = savedUnusedCoupon(now);
            tx.execute(s -> issuedCouponRepository.markUsedIfAvailable(coupon.getId(), MEMBER_ID, now, ORDER_ID));

            tx.executeWithoutResult(s -> issuedCouponRepository.restoreByOrderId(ORDER_ID));

            IssuedCoupon reloaded = issuedCouponRepository.findById(coupon.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(CouponStatus.UNUSED);
            assertThat(reloaded.getUsedOrderId()).isNull();
        }

        @Test
        @DisplayName("사용 이력 없는 주문으로 복원해도 멱등하게 무동작한다")
        void should_beIdempotent_when_noUsedCoupon() {
            ZonedDateTime now = ZonedDateTime.now();
            IssuedCoupon coupon = savedUnusedCoupon(now);

            tx.executeWithoutResult(s -> issuedCouponRepository.restoreByOrderId(9999L));

            assertThat(statusOf(coupon.getId())).isEqualTo(CouponStatus.UNUSED);
        }
    }
}
