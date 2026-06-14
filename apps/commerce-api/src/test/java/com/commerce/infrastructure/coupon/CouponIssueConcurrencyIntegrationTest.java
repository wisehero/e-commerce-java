package com.commerce.infrastructure.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.commerce.application.coupon.CouponIssueCommand;
import com.commerce.application.coupon.CouponIssueUseCase;
import com.commerce.domain.coupon.CouponPolicy;
import com.commerce.domain.coupon.CouponPolicyRepository;
import com.commerce.domain.coupon.DiscountRule;
import com.commerce.domain.coupon.DiscountType;
import com.commerce.domain.shared.Money;
import com.commerce.support.IntegrationTestSupport;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

class CouponIssueConcurrencyIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CouponIssueUseCase couponIssueUseCase;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private CouponPolicyJpaRepository couponPolicyJpaRepository;

    @Autowired
    private IssuedCouponJpaRepository issuedCouponJpaRepository;

    @AfterEach
    void tearDown() {
        issuedCouponJpaRepository.deleteAll();
        couponPolicyJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("동시에 발급해도 maxIssueCount를 초과하지 않는다")
    void should_notIssueMoreThanMaxIssueCount_when_concurrentIssue() throws Exception {
        // given
        CouponPolicy policy = couponPolicyRepository.save(policy(5L));
        int requestCount = 20;
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();

        var executor = Executors.newFixedThreadPool(requestCount);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < requestCount; i++) {
            long memberId = i + 1L;
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    couponIssueUseCase.issue(new CouponIssueCommand(policy.getId(), memberId));
                    successCount.incrementAndGet();
                } catch (CoreException ignored) {
                    // 소진 실패는 기대 가능한 경합 결과다.
                }
                return null;
            }));
        }

        // when
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        // then
        CouponPolicy reloaded = couponPolicyRepository.findById(policy.getId()).orElseThrow();
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(reloaded.getIssuedCount()).isEqualTo(5);
        assertThat(issuedCouponJpaRepository.count()).isEqualTo(5);
    }

    @Test
    @DisplayName("같은 회원은 같은 정책 쿠폰을 두 번 발급받을 수 없다")
    void should_throwConflict_when_sameMemberIssuesSamePolicyTwice() {
        // given
        CouponPolicy policy = couponPolicyRepository.save(policy(10L));
        couponIssueUseCase.issue(new CouponIssueCommand(policy.getId(), 1L));

        // when & then
        assertThatThrownBy(() -> couponIssueUseCase.issue(new CouponIssueCommand(policy.getId(), 1L)))
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.CONFLICT);

        CouponPolicy reloaded = couponPolicyRepository.findById(policy.getId()).orElseThrow();
        assertThat(reloaded.getIssuedCount()).isEqualTo(1);
        assertThat(issuedCouponJpaRepository.count()).isEqualTo(1);
    }

    private CouponPolicy policy(long maxIssueCount) {
        ZonedDateTime now = ZonedDateTime.now();
        return CouponPolicy.create(
            "선착순 쿠폰",
            new DiscountRule(DiscountType.FIXED, 1000L, null, Money.ZERO),
            7,
            now.minusHours(1),
            now.plusHours(1),
            maxIssueCount,
            true
        );
    }
}
