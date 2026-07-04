package com.commerce.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

class OrderTest {

    private static final Long MEMBER_ID = 1L;

    private static OrderLine line(long skuId, long unitPrice, int quantity) {
        return OrderLine.create(100L, skuId, "상품" + skuId, "옵션:기본", new Money(unitPrice), quantity);
    }

    private static Order placed() {
        return Order.place(MEMBER_ID, List.of(
            line(10L, 1000L, 2),    // 2000
            line(11L, 500L, 3)      // 1500
        ));
    }

    @Nested
    @DisplayName("place (신규 주문)")
    class Place {

        @Test
        @DisplayName("결제 대기 상태로 id 없이 생성되고 총액은 라인 합이다")
        void should_createPendingWithTotal_andNoId_when_place() {
            // when
            Order order = placed();

            // then
            assertThat(order)
                .satisfies(o -> assertThat(o.getId()).isNull())
                .satisfies(o -> assertThat(o.getMemberId()).isEqualTo(MEMBER_ID))
                .satisfies(o -> assertThat(o.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING))
                .satisfies(o -> assertThat(o.getOrderLines()).hasSize(2))
                .satisfies(o -> assertThat(o.getTotalAmount()).isEqualTo(new Money(3500)));
        }

        @Test
        @DisplayName("카트 기반 주문은 sourceCartId를 보관한다")
        void should_keepSourceCartId_when_cartSourceOrder() {
            // when
            Order order = Order.place(MEMBER_ID, List.of(line(10L, 1000L, 1)), Money.ZERO, null, 100L);

            // then
            assertThat(order.getSourceCartId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("주문자가 null이면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_memberIdNull() {
            // when & then
            assertThatThrownBy(() -> Order.place(null, List.of(line(10L, 1000L, 1))))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("주문 항목이 비어 있으면 BAD_REQUEST 예외가 발생한다")
        void should_throwException_when_linesEmpty() {
            // when & then
            assertThatThrownBy(() -> Order.place(MEMBER_ID, List.of()))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("markPaid (결제 완료)")
    class MarkPaid {

        @Test
        @DisplayName("결제 대기 주문을 결제 완료로 전이한다")
        void should_transitToPaid_when_pending() {
            // given
            Order order = placed();

            // when
            order.markPaid();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        }

        @Test
        @DisplayName("이미 결제 완료된 주문은 다시 결제 완료할 수 없다")
        void should_throwException_when_alreadyPaid() {
            // given
            Order order = placed();
            order.markPaid();

            // when & then
            assertThatThrownBy(order::markPaid)
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("취소된 주문은 결제 완료할 수 없다")
        void should_throwException_when_cancelled() {
            // given
            Order order = placed();
            order.cancel();

            // when & then
            assertThatThrownBy(order::markPaid)
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("totalAmountOf (주문 총액 계산)")
    class TotalAmountOf {

        @Test
        @DisplayName("주문 항목 금액의 합계를 계산한다")
        void should_calculateTotalAmountOfLines() {
            // given
            List<OrderLine> lines = List.of(
                line(10L, 1000L, 2),
                line(11L, 500L, 3)
            );

            // when
            Money totalAmount = Order.totalAmountOf(lines);

            // then
            assertThat(totalAmount).isEqualTo(new Money(3500));
        }
    }

    @Nested
    @DisplayName("cancel (취소)")
    class Cancel {

        @Test
        @DisplayName("결제 대기 주문을 취소할 수 있다 (결제 실패 보상)")
        void should_transitToCancelled_when_pending() {
            // given
            Order order = placed();

            // when
            order.cancel();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("결제 완료 주문을 취소할 수 있다 (유저 취소)")
        void should_transitToCancelled_when_paid() {
            // given
            Order order = placed();
            order.markPaid();

            // when
            order.cancel();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("이미 취소된 주문은 다시 취소할 수 없다")
        void should_throwException_when_alreadyCancelled() {
            // given
            Order order = placed();
            order.cancel();

            // when & then
            assertThatThrownBy(order::cancel)
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("reconstitute (영속 복원)")
    class Reconstitute {

        @Test
        @DisplayName("저장된 총액을 그대로 복원한다")
        void should_restoreStoredTotal_when_reconstitute() {
            // given
            List<OrderLine> lines = List.of(
                OrderLine.reconstitute(1L, 100L, 10L, "상품", "옵션:기본", new Money(1000), 2));

            // when
            Order order = Order.reconstitute(
                50L, MEMBER_ID, OrderStatus.PAID, lines, new Money(2000));

            // then
            assertThat(order.getId()).isEqualTo(50L);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(order.getTotalAmount()).isEqualTo(new Money(2000));
        }
    }
}
