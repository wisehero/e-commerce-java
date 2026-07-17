package com.commerce.application.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commerce.application.order.OrderInfo;
import com.commerce.application.order.OrderLineInfo;
import com.commerce.application.order.OrderPlaceCommand;
import com.commerce.application.order.OrderPlaceUseCase;
import com.commerce.domain.cart.Cart;
import com.commerce.domain.cart.CartRepository;
import com.commerce.domain.order.OrderStatus;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@ExtendWith(MockitoExtension.class)
class CartCheckoutUseCaseTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long CART_ID = 100L;
    private static final Long ORDER_ID = 1000L;
    private static final Long SKU_ID = 10L;

    @Mock
    private CartRepository cartRepository;
    @Mock
    private OrderPlaceUseCase orderPlaceUseCase;
    @Mock
    private CartCheckoutCleanupUseCase cleanupUseCase;

    private CartCheckoutUseCase useCase;

    @BeforeEach
    void setUp() {
        given(cleanupUseCase.cleanupPending(MEMBER_ID)).willReturn(true);
        useCase = new CartCheckoutUseCase(cartRepository, orderPlaceUseCase, cleanupUseCase);
    }

    private CartCheckoutCommand command() {
        return new CartCheckoutCommand(MEMBER_ID, "optimistic", null);
    }

    private Cart cartWithLine() {
        Cart cart = Cart.reconstitute(CART_ID, MEMBER_ID, List.of());
        cart.addItem(SKU_ID, 2);
        return cart;
    }

    private OrderInfo orderInfo(OrderStatus status, Long sourceCartId) {
        return new OrderInfo(ORDER_ID, MEMBER_ID, status.name(),
            List.of(new OrderLineInfo(1L, 100L, SKU_ID, "맨투맨", "색상:빨강", 8000L, 2, 16000L)),
            16000L, 0L, 16000L, null, sourceCartId);
    }

    @Nested
    @DisplayName("체크아웃 성공")
    class Success {

        @Test
        @DisplayName("PAID 주문이면 정리를 시도하고 정리 실패와 무관하게 주문을 반환한다")
        void should_returnPaidOrderAndAttemptCleanup_when_orderPaid() {
            // given
            given(cartRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(cartWithLine()));
            given(orderPlaceUseCase.place(any())).willReturn(orderInfo(OrderStatus.PAID, CART_ID));
            given(cleanupUseCase.cleanup(ORDER_ID)).willReturn(false);

            // when
            OrderInfo result = useCase.checkout(command());

            // then
            assertThat(result.status()).isEqualTo("PAID");
            assertThat(result.sourceCartId()).isEqualTo(CART_ID);
            then(cleanupUseCase).should().cleanup(ORDER_ID);

            ArgumentCaptor<OrderPlaceCommand> captor = ArgumentCaptor.forClass(OrderPlaceCommand.class);
            then(orderPlaceUseCase).should().place(captor.capture());
            OrderPlaceCommand placed = captor.getValue();
            assertThat(placed.memberId()).isEqualTo(MEMBER_ID);
            assertThat(placed.lockMode()).isEqualTo("optimistic");
            assertThat(placed.sourceCartId()).isEqualTo(CART_ID);
            assertThat(placed.lines()).hasSize(1);
            assertThat(placed.lines().getFirst().skuId()).isEqualTo(SKU_ID);
            assertThat(placed.lines().getFirst().quantity()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("체크아웃 실패/보상")
    class Failure {

        @Test
        @DisplayName("결제 실패로 CANCELLED 주문이면 카트를 유지한다")
        void should_keepCart_when_orderCancelledByPaymentFailure() {
            // given
            given(cartRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(cartWithLine()));
            given(orderPlaceUseCase.place(any())).willReturn(orderInfo(OrderStatus.CANCELLED, CART_ID));

            // when
            OrderInfo result = useCase.checkout(command());

            // then
            assertThat(result.status()).isEqualTo("CANCELLED");
            then(cleanupUseCase).should(never()).cleanup(any());
        }

        @Test
        @DisplayName("주문 생성이 실패하면 예외를 전파하고 카트를 유지한다")
        void should_keepCart_when_orderPlaceFails() {
            // given
            given(cartRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(cartWithLine()));
            given(orderPlaceUseCase.place(any()))
                .willThrow(new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다."));

            // when & then
            assertThatThrownBy(() -> useCase.checkout(command()))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
            then(cleanupUseCase).should(never()).cleanup(any());
        }
    }

    @Nested
    @DisplayName("체크아웃 검증")
    class Validation {

        @Test
        @DisplayName("카트가 없으면 NOT_FOUND 예외가 발생한다")
        void should_throwNotFound_when_cartMissing() {
            // given
            given(cartRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> useCase.checkout(command()))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
            then(orderPlaceUseCase).should(never()).place(any());
        }

        @Test
        @DisplayName("이전 카트 정리가 끝나지 않았으면 새 체크아웃을 CONFLICT로 거부한다")
        void should_rejectCheckout_when_previousCleanupPending() {
            // given
            given(cleanupUseCase.cleanupPending(MEMBER_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> useCase.checkout(command()))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.CONFLICT);
            then(orderPlaceUseCase).should(never()).place(any());
        }

        @Test
        @DisplayName("카트가 비어 있으면 BAD_REQUEST 예외가 발생한다")
        void should_throwBadRequest_when_cartEmpty() {
            // given
            given(cartRepository.findByMemberId(MEMBER_ID))
                .willReturn(Optional.of(Cart.reconstitute(CART_ID, MEMBER_ID, List.of())));

            // when & then
            assertThatThrownBy(() -> useCase.checkout(command()))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
            then(orderPlaceUseCase).should(never()).place(any());
        }
    }
}
