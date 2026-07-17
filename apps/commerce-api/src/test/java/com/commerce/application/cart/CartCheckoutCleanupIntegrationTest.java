package com.commerce.application.cart;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.commerce.domain.cart.Cart;
import com.commerce.domain.cart.CartCleanupTask;
import com.commerce.domain.cart.CartCleanupTaskRepository;
import com.commerce.domain.cart.CartRepository;
import com.commerce.domain.order.Order;
import com.commerce.domain.order.OrderLine;
import com.commerce.domain.order.OrderRepository;
import com.commerce.domain.shared.Money;
import com.commerce.infrastructure.cart.CartCleanupTaskJpaRepository;
import com.commerce.infrastructure.cart.CartJpaRepository;
import com.commerce.infrastructure.cart.CartLineJpaRepository;
import com.commerce.infrastructure.order.OrderJpaRepository;
import com.commerce.infrastructure.order.OrderLineJpaRepository;
import com.commerce.support.IntegrationTestSupport;

class CartCheckoutCleanupIntegrationTest extends IntegrationTestSupport {

    private static final Long MEMBER_ID = 1L;
    private static final Long PURCHASED_SKU_ID = 10L;
    private static final Long NEW_SKU_ID = 11L;

    @Autowired
    private CartCheckoutCleanupUseCase cleanupUseCase;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CartCleanupTaskRepository cleanupTaskRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CartCleanupTaskJpaRepository cleanupTaskJpaRepository;
    @Autowired
    private CartLineJpaRepository cartLineJpaRepository;
    @Autowired
    private CartJpaRepository cartJpaRepository;
    @Autowired
    private OrderLineJpaRepository orderLineJpaRepository;
    @Autowired
    private OrderJpaRepository orderJpaRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void tearDown() {
        cleanupTaskJpaRepository.deleteAll();
        orderLineJpaRepository.deleteAll();
        orderJpaRepository.deleteAll();
        cartLineJpaRepository.deleteAll();
        cartJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("정리 작업과 카트 수량 변경을 함께 커밋하고 재실행은 멱등하다")
    void should_cleanupCartAndCompleteTaskIdempotently() {
        // given — 주문 2개, 결제 중 같은 SKU 1개와 다른 SKU 1개가 추가된 현재 카트
        CheckoutFixture fixture = createPaidCheckoutFixture();

        // when
        boolean first = cleanupUseCase.cleanup(fixture.orderId());
        boolean second = cleanupUseCase.cleanup(fixture.orderId());

        // then
        assertThat(first).isTrue();
        assertThat(second).isTrue();

        Cart cleaned = cartRepository.findByMemberId(MEMBER_ID).orElseThrow();
        assertThat(cleaned.quantityOf(PURCHASED_SKU_ID)).isEqualTo(1);
        assertThat(cleaned.quantityOf(NEW_SKU_ID)).isEqualTo(1);
        assertThat(cleanupTaskRepository.findByOrderId(fixture.orderId()).orElseThrow().isCompleted()).isTrue();
    }

    @Test
    @DisplayName("두 실행자가 같은 작업을 동시에 잡아도 주문 수량은 한 번만 차감한다")
    void should_subtractOnlyOnce_when_sameTaskRunsConcurrently() throws Exception {
        // given
        CheckoutFixture fixture = createPaidCheckoutFixture();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Boolean> first = executor.submit(() -> cleanupAfterSignal(fixture.orderId(), ready, start));
            Future<Boolean> second = executor.submit(() -> cleanupAfterSignal(fixture.orderId(), ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

            // when
            start.countDown();

            // then
            assertThat(first.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(second.get(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        Cart cleaned = cartRepository.findByMemberId(MEMBER_ID).orElseThrow();
        assertThat(cleaned.quantityOf(PURCHASED_SKU_ID)).isEqualTo(1);
        assertThat(cleanupTaskRepository.findByOrderId(fixture.orderId()).orElseThrow().isCompleted()).isTrue();
    }

    @Test
    @DisplayName("같은 SKU 추가와 정리가 동시에 실행돼도 추가 수량을 잃지 않는다")
    void should_keepAddedQuantity_when_addAndCleanupRunConcurrently() throws Exception {
        // given
        CheckoutFixture fixture = createPaidCheckoutFixture();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Boolean> cleanup = executor.submit(() -> cleanupAfterSignal(fixture.orderId(), ready, start));
            Future<?> add = executor.submit(() -> addItemAfterSignal(fixture.cartId(), ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

            // when
            start.countDown();

            // then
            assertThat(cleanup.get(10, TimeUnit.SECONDS)).isTrue();
            add.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        Cart cleaned = cartRepository.findByMemberId(MEMBER_ID).orElseThrow();
        assertThat(cleaned.quantityOf(PURCHASED_SKU_ID)).isEqualTo(2);
        assertThat(cleaned.quantityOf(NEW_SKU_ID)).isEqualTo(1);
    }

    @Test
    @DisplayName("주문 취소가 먼저 잠금을 잡으면 카트를 유지하고 작업만 완료한다")
    void should_keepCart_when_cancellationWinsOrderLock() throws Exception {
        // given
        CheckoutFixture fixture = createPaidCheckoutFixture();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch cancellationUpdated = new CountDownLatch(1);
        CountDownLatch allowCancellationCommit = new CountDownLatch(1);

        try {
            Future<?> cancellation = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                Order order = orderRepository.findByIdForUpdate(fixture.orderId()).orElseThrow();
                order.cancel();
                orderRepository.save(order);
                cancellationUpdated.countDown();
                await(allowCancellationCommit);
            }));
            assertThat(cancellationUpdated.await(5, TimeUnit.SECONDS)).isTrue();
            Future<Boolean> cleanup = executor.submit(() -> cleanupUseCase.cleanup(fixture.orderId()));

            // when
            allowCancellationCommit.countDown();

            // then
            cancellation.get(10, TimeUnit.SECONDS);
            assertThat(cleanup.get(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        Cart kept = cartRepository.findByMemberId(MEMBER_ID).orElseThrow();
        assertThat(kept.quantityOf(PURCHASED_SKU_ID)).isEqualTo(3);
        assertThat(kept.quantityOf(NEW_SKU_ID)).isEqualTo(1);
        assertThat(cleanupTaskRepository.findByOrderId(fixture.orderId()).orElseThrow().isCompleted()).isTrue();
    }

    @Test
    @DisplayName("실행 시각이 된 PENDING 작업만 ready 조회에 포함한다")
    void should_findOnlyDuePendingTasks() {
        // given
        ZonedDateTime now = ZonedDateTime.now();
        CartCleanupTask due = cleanupTaskRepository.save(CartCleanupTask.pending(9001L, 8001L, 7001L,
            now.minusSeconds(1)));
        cleanupTaskRepository.save(CartCleanupTask.pending(9002L, 8002L, 7002L, now.plusMinutes(1)));
        CartCleanupTask completed = cleanupTaskRepository.save(CartCleanupTask.pending(9003L, 8003L, 7003L,
            now.minusSeconds(1)));
        completed.complete(now);
        cleanupTaskRepository.save(completed);

        // when & then
        assertThat(cleanupTaskRepository.findReady(now))
            .extracting(CartCleanupTask::getId)
            .containsExactly(due.getId());
    }

    private CheckoutFixture createPaidCheckoutFixture() {
        Cart cart = Cart.create(MEMBER_ID);
        cart.addItem(PURCHASED_SKU_ID, 3);
        cart.addItem(NEW_SKU_ID, 1);
        Cart savedCart = cartRepository.save(cart);

        OrderLine purchasedLine = OrderLine.create(100L, PURCHASED_SKU_ID, "맨투맨", "색상:블랙",
            new Money(8000), 2);
        Order pendingOrder = orderRepository.save(Order.place(MEMBER_ID, List.of(purchasedLine), Money.ZERO, null,
            savedCart.getId()));
        transactionTemplate.executeWithoutResult(status -> {
            Order order = orderRepository.findByIdForUpdate(pendingOrder.getId()).orElseThrow();
            order.markPaid();
            orderRepository.save(order);
        });
        cleanupTaskRepository.save(CartCleanupTask.pending(pendingOrder.getId(), savedCart.getId(), MEMBER_ID,
            ZonedDateTime.now()));
        return new CheckoutFixture(pendingOrder.getId(), savedCart.getId());
    }

    private boolean cleanupAfterSignal(Long orderId, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        await(start);
        return cleanupUseCase.cleanup(orderId);
    }

    private void addItemAfterSignal(Long cartId, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        await(start);
        transactionTemplate.executeWithoutResult(status -> {
            Cart cart = cartRepository.findByIdForUpdate(cartId).orElseThrow();
            cart.addItem(PURCHASED_SKU_ID, 1);
            cartRepository.save(cart);
        });
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent test signal");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for concurrent test signal", exception);
        }
    }

    private record CheckoutFixture(Long orderId, Long cartId) {}
}
