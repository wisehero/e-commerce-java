package com.commerce.domain.order;

import java.util.Optional;

import com.commerce.support.page.PageResult;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    PageResult<Order> findByMemberId(Long memberId, int page, int size);
}
