package com.commerce.application.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.commerce.domain.cart.Cart;
import com.commerce.domain.cart.CartCleanupTask;
import com.commerce.domain.cart.CartCleanupTaskRepository;
import com.commerce.domain.cart.CartCleanupTaskStatus;
import com.commerce.domain.cart.CartRepository;
import com.commerce.domain.order.Order;
import com.commerce.domain.order.OrderLine;
import com.commerce.domain.order.OrderRepository;
import com.commerce.domain.order.OrderStatus;
import com.commerce.domain.shared.Money;

@ExtendWith(MockitoExtension.class)
class CartCheckoutCleanupUseCaseTest {

    private static final Long TASK_ID = 1L;
    private static final Long ORDER_ID = 100L;
    private static final Long CART_ID = 10L;
    private static final Long MEMBER_ID = 5L;
    private static final Long SKU_ID = 20L;

    @Mock
    private CartCleanupTaskRepository cleanupTaskRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private CartCheckoutCleanupUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        useCase = new CartCheckoutCleanupUseCase(cleanupTaskRepository, cartRepository, orderRepository,
            transactionManager);
    }

    private CartCleanupTask pendingTask() {
        return CartCleanupTask.reconstitute(TASK_ID, ORDER_ID, CART_ID, MEMBER_ID,
            CartCleanupTaskStatus.PENDING, 0, ZonedDateTime.now(), null, null);
    }

    private Order paidOrder() {
        OrderLine line = OrderLine.reconstitute(1L, 200L, SKU_ID, "맨투맨", "색상:블랙", new Money(8000), 2);
        return Order.reconstitute(ORDER_ID, MEMBER_ID, OrderStatus.PAID, List.of(line), new Money(16000),
            Money.ZERO, new Money(16000), null, CART_ID);
    }

    private Cart cartWithQuantities() {
        Cart cart = Cart.reconstitute(CART_ID, MEMBER_ID, List.of());
        cart.addItem(SKU_ID, 3);
        cart.addItem(21L, 1);
        return cart;
    }

    @Test
    @DisplayName("PAID 주문 수량만 차감하고 정리 작업을 완료한다")
    void should_subtractPurchasedQuantityAndCompleteTask_when_paidOrder() {
        // given
        CartCleanupTask task = pendingTask();
        Cart cart = cartWithQuantities();
        given(cleanupTaskRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(task));
        given(cleanupTaskRepository.findByIdForUpdate(TASK_ID)).willReturn(Optional.of(task));
        given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(paidOrder()));
        given(cartRepository.findByIdForUpdate(CART_ID)).willReturn(Optional.of(cart));
        given(cartRepository.save(cart)).willReturn(cart);
        given(cleanupTaskRepository.save(task)).willReturn(task);

        // when
        boolean completed = useCase.cleanup(ORDER_ID);

        // then
        assertThat(completed).isTrue();
        assertThat(cart.quantityOf(SKU_ID)).isEqualTo(1);
        assertThat(cart.quantityOf(21L)).isEqualTo(1);
        assertThat(task.isCompleted()).isTrue();
        then(cartRepository).should().save(cart);
        then(cleanupTaskRepository).should().save(task);
    }

    @Test
    @DisplayName("이미 완료된 작업은 카트를 다시 차감하지 않는다")
    void should_doNothing_when_taskAlreadyCompleted() {
        // given
        ZonedDateTime now = ZonedDateTime.now();
        CartCleanupTask completed = CartCleanupTask.reconstitute(TASK_ID, ORDER_ID, CART_ID, MEMBER_ID,
            CartCleanupTaskStatus.COMPLETED, 0, now, null, now);
        given(cleanupTaskRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(completed));
        given(cleanupTaskRepository.findByIdForUpdate(TASK_ID)).willReturn(Optional.of(completed));

        // when
        boolean result = useCase.cleanup(ORDER_ID);

        // then
        assertThat(result).isTrue();
        then(orderRepository).should(never()).findByIdForUpdate(any());
        then(cartRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("정리 실패는 외부로 전파하지 않고 다음 재시도 정보를 기록한다")
    void should_recordRetryAndReturnFalse_when_cleanupFails() {
        // given
        CartCleanupTask task = pendingTask();
        given(cleanupTaskRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(task));
        given(cleanupTaskRepository.findByIdForUpdate(TASK_ID)).willReturn(Optional.of(task));
        given(orderRepository.findByIdForUpdate(ORDER_ID)).willThrow(new IllegalStateException("temporary DB failure"));
        given(cleanupTaskRepository.save(task)).willReturn(task);

        // when
        boolean completed = useCase.cleanup(ORDER_ID);

        // then
        assertThat(completed).isFalse();
        assertThat(task.isPending()).isTrue();
        assertThat(task.getAttemptCount()).isEqualTo(1);
        assertThat(task.getLastError()).isEqualTo("temporary DB failure");
        then(cleanupTaskRepository).should().save(task);
    }

    @Test
    @DisplayName("정리 작업 조회 장애도 결제 응답으로 전파하지 않는다")
    void should_returnFalse_when_cleanupTaskLookupFails() {
        // given
        given(cleanupTaskRepository.findByOrderId(ORDER_ID))
            .willThrow(new IllegalStateException("temporary DB failure"));

        // when
        boolean completed = useCase.cleanup(ORDER_ID);

        // then
        assertThat(completed).isFalse();
        then(cleanupTaskRepository).should(never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("실행 시각이 된 작업은 재시도 진입점에서도 정리한다")
    void should_cleanupReadyTasks_when_retryRuns() {
        // given
        CartCleanupTask task = pendingTask();
        Cart cart = cartWithQuantities();
        given(cleanupTaskRepository.findReady(any())).willReturn(List.of(task));
        given(cleanupTaskRepository.findByIdForUpdate(TASK_ID)).willReturn(Optional.of(task));
        given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(paidOrder()));
        given(cartRepository.findByIdForUpdate(CART_ID)).willReturn(Optional.of(cart));
        given(cartRepository.save(cart)).willReturn(cart);
        given(cleanupTaskRepository.save(task)).willReturn(task);

        // when
        useCase.retryReady();

        // then
        assertThat(task.isCompleted()).isTrue();
        assertThat(cart.quantityOf(SKU_ID)).isEqualTo(1);
    }

    @Test
    @DisplayName("회원의 이전 작업을 즉시 정리하고 미완료 작업이 없으면 새 체크아웃을 허용한다")
    void should_returnTrue_when_memberPendingTasksAreCleaned() {
        // given
        CartCleanupTask task = pendingTask();
        Cart cart = cartWithQuantities();
        given(cleanupTaskRepository.findPendingByMemberId(MEMBER_ID)).willReturn(List.of(task));
        given(cleanupTaskRepository.findByIdForUpdate(TASK_ID)).willReturn(Optional.of(task));
        given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(paidOrder()));
        given(cartRepository.findByIdForUpdate(CART_ID)).willReturn(Optional.of(cart));
        given(cartRepository.save(cart)).willReturn(cart);
        given(cleanupTaskRepository.save(task)).willReturn(task);
        given(cleanupTaskRepository.existsPendingByMemberId(MEMBER_ID)).willReturn(false);

        // when
        boolean readyForCheckout = useCase.cleanupPending(MEMBER_ID);

        // then
        assertThat(readyForCheckout).isTrue();
        assertThat(task.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("원본 카트가 이미 없으면 정리가 끝난 것으로 처리한다")
    void should_completeTask_when_sourceCartAlreadyMissing() {
        // given
        CartCleanupTask task = pendingTask();
        given(cleanupTaskRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(task));
        given(cleanupTaskRepository.findByIdForUpdate(TASK_ID)).willReturn(Optional.of(task));
        given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(paidOrder()));
        given(cartRepository.findByIdForUpdate(CART_ID)).willReturn(Optional.empty());
        given(cleanupTaskRepository.save(task)).willReturn(task);

        // when
        boolean completed = useCase.cleanup(ORDER_ID);

        // then
        assertThat(completed).isTrue();
        assertThat(task.isCompleted()).isTrue();
        then(cartRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("정리 전에 주문이 취소됐으면 카트는 유지하고 작업만 완료한다")
    void should_completeTaskWithoutCartMutation_when_orderWasCancelled() {
        // given
        CartCleanupTask task = pendingTask();
        Order cancelledOrder = Order.reconstitute(ORDER_ID, MEMBER_ID, OrderStatus.CANCELLED,
            paidOrder().getOrderLines(), new Money(16000), Money.ZERO, new Money(16000), null, CART_ID);
        given(cleanupTaskRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(task));
        given(cleanupTaskRepository.findByIdForUpdate(TASK_ID)).willReturn(Optional.of(task));
        given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(cancelledOrder));
        given(cleanupTaskRepository.save(task)).willReturn(task);

        // when
        boolean completed = useCase.cleanup(ORDER_ID);

        // then
        assertThat(completed).isTrue();
        assertThat(task.isCompleted()).isTrue();
        then(cartRepository).should(never()).findByIdForUpdate(any());
        then(cartRepository).should(never()).save(any());
    }
}
