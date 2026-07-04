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
 * 서버 주도 장바구니 체크아웃. Cart와 Order 도메인을 직접 결합하지 않고 application 계층에서 오케스트레이션한다.
 */
@Service
@RequiredArgsConstructor
public class CartCheckoutUseCase {

    private final CartRepository cartRepository;
    private final OrderPlaceUseCase orderPlaceUseCase;
    private final CartClearUseCase cartClearUseCase;

    public OrderInfo checkout(CartCheckoutCommand command) {
        Cart cart = cartRepository.findByMemberId(command.memberId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "장바구니가 없습니다."));
        if (cart.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "장바구니가 비어 있습니다.");
        }

        OrderInfo order = orderPlaceUseCase.place(toOrderPlaceCommand(command, cart));
        if (OrderStatus.PAID.name().equals(order.status())) {
            cartClearUseCase.clear(command.memberId());
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
