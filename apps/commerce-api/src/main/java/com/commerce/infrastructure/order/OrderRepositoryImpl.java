package com.commerce.infrastructure.order;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.commerce.domain.order.Order;
import com.commerce.domain.order.OrderLine;
import com.commerce.domain.order.OrderRepository;
import com.commerce.support.page.PageResult;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpa;
    private final OrderLineJpaRepository orderLineJpa;

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            return insert(order);
        }
        return updateStatus(order);
    }

    /** 신규: orders 저장 → 생성된 id로 order_lines 저장 → 조립 */
    private Order insert(Order order) {
        OrderJpaEntity savedOrder = orderJpa.save(OrderJpaEntity.fromDomain(order));
        Long orderId = savedOrder.getId();

        List<OrderLineJpaEntity> lineEntities = order.getOrderLines().stream()
            .map(line -> OrderLineJpaEntity.fromDomain(orderId, line))
            .toList();
        List<OrderLine> savedLines = orderLineJpa.saveAll(lineEntities).stream()
            .map(OrderLineJpaEntity::toDomain)
            .toList();

        return savedOrder.toDomain(savedLines);
    }

    /** 기존: 상태 전이만 반영(라인 불변). 더티체킹으로 flush */
    private Order updateStatus(Order order) {
        OrderJpaEntity existing = orderJpa.findById(order.getId())
            .orElseThrow(() -> new IllegalStateException("Order not found: " + order.getId()));
        existing.updateFromDomain(order);

        List<OrderLine> lines = orderLineJpa.findByOrderId(order.getId()).stream()
            .map(OrderLineJpaEntity::toDomain)
            .toList();
        return existing.toDomain(lines);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderJpa.findById(id)
            .map(entity -> entity.toDomain(linesOf(id)));
    }

    @Override
    public Optional<Order> findByIdForUpdate(Long id) {
        return orderJpa.findByIdForUpdate(id)
            .map(entity -> entity.toDomain(linesOf(id)));
    }

    @Override
    public PageResult<Order> findByMemberId(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<OrderJpaEntity> orderPage = orderJpa.findByMemberId(memberId, pageable);

        List<Long> orderIds = orderPage.getContent().stream()
            .map(OrderJpaEntity::getId)
            .toList();
        Map<Long, List<OrderLine>> linesByOrderId = linesGroupedBy(orderIds);

        List<Order> items = orderPage.getContent().stream()
            .map(entity -> entity.toDomain(linesByOrderId.getOrDefault(entity.getId(), List.of())))
            .toList();

        return new PageResult<>(items, orderPage.getTotalElements(), page, size);
    }

    private List<OrderLine> linesOf(Long orderId) {
        return orderLineJpa.findByOrderId(orderId).stream()
            .map(OrderLineJpaEntity::toDomain)
            .toList();
    }

    private Map<Long, List<OrderLine>> linesGroupedBy(List<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();                        // IN () 쿼리 방지
        }
        return orderLineJpa.findByOrderIdIn(orderIds).stream()
            .collect(Collectors.groupingBy(
                OrderLineJpaEntity::getOrderId,
                Collectors.mapping(OrderLineJpaEntity::toDomain, Collectors.toList())
            ));
    }
}
