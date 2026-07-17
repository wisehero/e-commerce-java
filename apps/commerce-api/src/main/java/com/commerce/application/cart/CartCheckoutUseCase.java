package com.commerce.application.cart;

import java.util.List;

import org.springframework.stereotype.Service;

import com.commerce.application.order.OrderInfo;
import com.commerce.application.order.OrderPlaceCommand;
import com.commerce.application.order.OrderPlaceUseCase;
import com.commerce.domain.cart.Cart;
import com.commerce.domain.cart.CartLine;
import com.commerce.domain.cart.CartRepository;
import com.commerce.domain.order.OrderStatus;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * 서버 주도 장바구니 체크아웃. PAID 주문은 내구성 정리 작업을 즉시 시도하되,
 * 정리 실패가 이미 확정된 결제 결과를 실패 응답으로 바꾸지 않도록 application 계층에서 오케스트레이션한다.
 */
@Service
@RequiredArgsConstructor
public class CartCheckoutUseCase {

    private final CartRepository cartRepository;
    private final OrderPlaceUseCase orderPlaceUseCase;
    private final CartCheckoutCleanupUseCase cleanupUseCase;

    public OrderInfo checkout(CartCheckoutCommand command) {
        if (!cleanupUseCase.cleanupPending(command.memberId())) {
            throw new CoreException(ErrorType.CONFLICT, "이전 장바구니 주문을 정리 중입니다. 잠시 후 다시 시도해주세요.");
        }

        Cart cart = cartRepository.findByMemberId(command.memberId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "장바구니가 없습니다."));
        if (cart.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "장바구니가 비어 있습니다.");
        }

        OrderInfo order = orderPlaceUseCase.place(toOrderPlaceCommand(command, cart));
        if (OrderStatus.PAID.name().equals(order.status())) {
            cleanupUseCase.cleanup(order.id());
        }
        return order;
    }

    private OrderPlaceCommand toOrderPlaceCommand(CartCheckoutCommand command, Cart cart) {
        List<OrderPlaceCommand.LineCommand> lines = cart.getLines().stream()
            .map(this::toLineCommand)
            .toList();
        return new OrderPlaceCommand(command.memberId(), lines, command.lockMode(), command.couponId(), cart.getId());
    }

    private OrderPlaceCommand.LineCommand toLineCommand(CartLine line) {
        return new OrderPlaceCommand.LineCommand(line.getSkuId(), line.getQuantity());
    }
}
