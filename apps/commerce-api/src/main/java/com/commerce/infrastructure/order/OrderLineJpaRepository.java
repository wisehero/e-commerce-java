package com.commerce.infrastructure.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderLineJpaRepository extends JpaRepository<OrderLineJpaEntity, Long> {

    List<OrderLineJpaEntity> findByOrderId(Long orderId);

    List<OrderLineJpaEntity> findByOrderIdIn(List<Long> orderIds);
}
