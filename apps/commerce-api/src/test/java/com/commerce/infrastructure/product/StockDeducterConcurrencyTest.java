package com.commerce.infrastructure.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.commerce.domain.product.OptionValue;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.Stock;
import com.commerce.domain.product.StockDeducter;
import com.commerce.domain.shared.Money;
import com.commerce.support.IntegrationTestSupport;

/**
 * 재고 차감 3전략의 동시성 검증. 실 MySQL에서 N스레드가 같은 SKU를 동시에 차감해
 * 오버셀(재고 음수)이 없음을, 그리고 전략별 처리량 차이를 확인한다.
 */
class StockDeducterConcurrencyTest extends IntegrationTestSupport {

    private static final int INITIAL_STOCK = 20;
    private static final int CONCURRENCY = 50;

    @Autowired
    private Map<String, StockDeducter> stockDeducters;

    @Autowired
    private SkuJpaRepository skuJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @BeforeEach
    void setUp() {
        txTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void tearDown() {
        skuJpaRepository.deleteAll();
    }

    private Long seedSku(int stock) {
        Sku sku = Sku.create(1L, List.of(new OptionValue("색상", "빨강")), new Money(10000), new Stock(stock));
        return skuJpaRepository.save(SkuJpaEntity.fromDomain(sku)).getId();
    }

    private DeductResult runConcurrentDeduct(String lockMode, Long skuId) throws InterruptedException {
        StockDeducter deducter = stockDeducters.get(lockMode);
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        CountDownLatch ready = new CountDownLatch(CONCURRENCY);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();

        for (int i = 0; i < CONCURRENCY; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    txTemplate.executeWithoutResult(s -> deducter.deduct(skuId, 1));
                    success.incrementAndGet();
                } catch (Exception e) {
                    failure.incrementAndGet();
                }
            });
        }
        ready.await();
        start.countDown();                 // 모든 스레드를 동시에 출발시켜 경합 극대화
        pool.shutdown();
        pool.awaitTermination(60, TimeUnit.SECONDS);

        return new DeductResult(success.get(), failure.get());
    }

    private int currentStock(Long skuId) {
        return skuJpaRepository.findById(skuId).orElseThrow().getStock();
    }

    private record DeductResult(int success, int failure) {
    }

    @ParameterizedTest
    @ValueSource(strings = {"optimistic", "pessimistic", "atomic"})
    @DisplayName("동시 차감에도 오버셀이 없다 (재고 음수 불가, 성공수 = 차감량)")
    void should_notOversell_underConcurrency(String lockMode) throws InterruptedException {
        // given
        Long skuId = seedSku(INITIAL_STOCK);

        // when
        DeductResult result = runConcurrentDeduct(lockMode, skuId);

        // then
        int finalStock = currentStock(skuId);
        assertThat(finalStock).isGreaterThanOrEqualTo(0);                  // 오버셀 없음
        assertThat(result.success()).isEqualTo(INITIAL_STOCK - finalStock); // 성공수 = 실제 차감량
        assertThat(result.success()).isLessThanOrEqualTo(INITIAL_STOCK);
        assertThat(result.success() + result.failure()).isEqualTo(CONCURRENCY);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pessimistic", "atomic"})
    @DisplayName("비관적 락·원자 UPDATE는 재고를 정확히 소진한다 (성공수 = 초기재고, 잔여 = 0)")
    void should_sellOutExactly_when_pessimisticOrAtomic(String lockMode) throws InterruptedException {
        // given
        Long skuId = seedSku(INITIAL_STOCK);

        // when
        DeductResult result = runConcurrentDeduct(lockMode, skuId);

        // then
        assertThat(currentStock(skuId)).isZero();
        assertThat(result.success()).isEqualTo(INITIAL_STOCK);
        assertThat(result.failure()).isEqualTo(CONCURRENCY - INITIAL_STOCK);
    }
}
