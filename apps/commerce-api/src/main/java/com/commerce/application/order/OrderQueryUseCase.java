package com.commerce.application.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.order.Order;
import com.commerce.domain.order.OrderRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;
import com.commerce.support.page.PageQuery;
import com.commerce.support.page.PageResult;

import lombok.RequiredArgsConstructor;

/**
 * 주문 조회 유스케이스. 외부 호출이 없어 선언적 @Transactional(readOnly)로 충분하다.
 */
@Service
@RequiredArgsConstructor
public class OrderQueryUseCase {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public OrderInfo getById(Long memberId, Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다."));
        ensureOwner(order, memberId);
        return OrderInfo.from(order);
    }

    @Transactional(readOnly = true)
    public PageResult<OrderInfo> getByMember(Long memberId, int page, int size) {
        PageQuery pageQuery = new PageQuery(page, size);
        PageResult<Order> orders = orderRepository.findByMemberId(memberId, pageQuery.page(), pageQuery.size());
        return new PageResult<>(
            orders.items().stream().map(OrderInfo::from).toList(),
            orders.totalCount(), orders.page(), orders.size()
        );
    }

    private void ensureOwner(Order order, Long memberId) {
        if (!order.getMemberId().equals(memberId)) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다.");
        }
    }
}
