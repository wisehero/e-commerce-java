package com.commerce.application.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commerce.domain.order.Order;
import com.commerce.domain.order.OrderLine;
import com.commerce.domain.order.OrderRepository;
import com.commerce.domain.order.OrderStatus;
import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;
import com.commerce.support.page.PageResult;

@ExtendWith(MockitoExtension.class)
class OrderQueryUseCaseTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;
    private static final Long ORDER_ID = 1000L;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderQueryUseCase useCase;

    private Order order() {
        OrderLine line = OrderLine.reconstitute(1L, 100L, 10L, "맨투맨", "색상:빨강", new Money(8000), 2);
        return Order.reconstitute(ORDER_ID, MEMBER_ID, OrderStatus.PAID, List.of(line), new Money(16000));
    }

    @Nested
    @DisplayName("단건 조회")
    class GetById {

        @Test
        @DisplayName("존재하는 주문이면 OrderInfo를 반환한다")
        void should_returnInfo_when_found() {
            // given
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order()));

            // when
            OrderInfo info = useCase.getById(MEMBER_ID, ORDER_ID);

            // then
            assertThat(info.id()).isEqualTo(ORDER_ID);
            assertThat(info.lines()).hasSize(1);
        }

        @Test
        @DisplayName("존재하지 않는 주문이면 NOT_FOUND 예외가 발생한다")
        void should_throwNotFound_when_missing() {
            // given
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> useCase.getById(MEMBER_ID, ORDER_ID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
        }

        @Test
        @DisplayName("다른 회원의 주문이면 NOT_FOUND 예외가 발생한다")
        void should_throwNotFound_when_otherMemberOrder() {
            // given
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order()));

            // when & then
            assertThatThrownBy(() -> useCase.getById(OTHER_MEMBER_ID, ORDER_ID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("회원별 목록 조회")
    class GetByMember {

        @Test
        @DisplayName("회원 주문 목록을 PageResult<OrderInfo>로 매핑해 반환한다")
        void should_returnMappedPage_when_getByMember() {
            // given
            given(orderRepository.findByMemberId(MEMBER_ID, 0, 10))
                .willReturn(new PageResult<>(List.of(order()), 1L, 0, 10));

            // when
            PageResult<OrderInfo> result = useCase.getByMember(MEMBER_ID, 0, 10);

            // then
            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).id()).isEqualTo(ORDER_ID);
            assertThat(result.totalCount()).isEqualTo(1L);
            assertThat(result.page()).isZero();
        }

        @Test
        @DisplayName("page가 음수면 repository 호출 전에 BAD_REQUEST로 막는다")
        void should_throwBadRequest_when_negativePage() {
            assertThatThrownBy(() -> useCase.getByMember(MEMBER_ID, -1, 10))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);

            then(orderRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("size가 0이면 repository 호출 전에 BAD_REQUEST로 막는다")
        void should_throwBadRequest_when_zeroSize() {
            assertThatThrownBy(() -> useCase.getByMember(MEMBER_ID, 0, 0))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);

            then(orderRepository).shouldHaveNoInteractions();
        }
    }
}
