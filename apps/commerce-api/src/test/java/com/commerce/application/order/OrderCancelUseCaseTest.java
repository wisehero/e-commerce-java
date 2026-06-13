package com.commerce.application.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.commerce.domain.order.Order;
import com.commerce.domain.order.OrderLine;
import com.commerce.domain.order.OrderRepository;
import com.commerce.domain.order.OrderStatus;
import com.commerce.domain.order.PaymentGateway;
import com.commerce.domain.product.OptionValue;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.domain.shared.Money;
import com.commerce.domain.product.Stock;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@ExtendWith(MockitoExtension.class)
class OrderCancelUseCaseTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long SKU_ID = 10L;
    private static final Long PRODUCT_ID = 100L;
    private static final Long ORDER_ID = 1000L;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private SkuRepository skuRepository;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private PlatformTransactionManager transactionManager;

    private OrderCancelUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        useCase = new OrderCancelUseCase(orderRepository, skuRepository, paymentGateway, transactionManager);
    }

    private Sku sku() {
        return Sku.reconstitute(SKU_ID, PRODUCT_ID, List.of(new OptionValue("색상", "빨강")),
            new Money(10000), new Money(8000), new Stock(100));
    }

    private Order orderWith(OrderStatus status) {
        OrderLine line = OrderLine.reconstitute(1L, PRODUCT_ID, SKU_ID, "맨투맨", "색상:빨강", new Money(8000), 2);
        return Order.reconstitute(ORDER_ID, MEMBER_ID, status, List.of(line), new Money(16000));
    }

    private void givenStockRestorable() {
        given(skuRepository.findById(SKU_ID)).willReturn(Optional.of(sku()));
        given(skuRepository.save(any(Sku.class))).willAnswer(inv -> inv.getArgument(0));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> {
            Order o = inv.getArgument(0);
            return Order.reconstitute(ORDER_ID, o.getMemberId(), o.getStatus(), o.getOrderLines(), o.getTotalAmount());
        });
    }

    @Nested
    @DisplayName("주문 취소")
    class Cancel {

        @Test
        @DisplayName("결제된 주문을 취소하면 재고를 복원하고 환불한다")
        void should_restoreStockAndRefund_when_paidOrder() {
            // given
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(orderWith(OrderStatus.PAID)));
            givenStockRestorable();

            // when
            OrderInfo info = useCase.cancel(ORDER_ID);

            // then
            assertThat(info.status()).isEqualTo("CANCELLED");
            then(skuRepository).should().save(any(Sku.class));
            then(paymentGateway).should().refund(eq(ORDER_ID), any());
        }

        @Test
        @DisplayName("결제 전(PAYMENT_PENDING) 주문을 취소하면 재고는 복원하되 환불은 하지 않는다")
        void should_notRefund_when_pendingOrder() {
            // given
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(orderWith(OrderStatus.PAYMENT_PENDING)));
            givenStockRestorable();

            // when
            OrderInfo info = useCase.cancel(ORDER_ID);

            // then
            assertThat(info.status()).isEqualTo("CANCELLED");
            then(skuRepository).should().save(any(Sku.class));
            then(paymentGateway).should(never()).refund(any(), any());
        }

        @Test
        @DisplayName("이미 취소된 주문은 BAD_REQUEST 예외가 발생하고 환불하지 않는다")
        void should_throwBadRequest_when_alreadyCancelled() {
            // given
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(orderWith(OrderStatus.CANCELLED)));

            // when & then
            assertThatThrownBy(() -> useCase.cancel(ORDER_ID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
            then(paymentGateway).should(never()).refund(any(), any());
        }

        @Test
        @DisplayName("존재하지 않는 주문이면 NOT_FOUND 예외가 발생한다")
        void should_throwNotFound_when_orderMissing() {
            // given
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> useCase.cancel(ORDER_ID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
        }
    }
}
