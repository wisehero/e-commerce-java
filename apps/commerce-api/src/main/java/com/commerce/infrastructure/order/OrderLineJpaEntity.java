package com.commerce.infrastructure.order;

import com.commerce.domain.order.OrderLine;
import com.commerce.domain.shared.Money;
import com.commerce.infrastructure.jpa.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 라인 매핑. Order Aggregate의 자식이며 order_id로 루트를 ID 참조한다.
 * (JPA 연관 어노테이션 미사용 — OrderRepositoryImpl이 명시적으로 저장/조회)
 */
@Entity
@Table(name = "order_lines", indexes = {
    @Index(name = "idx_order_lines_order_id", columnList = "order_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderLineJpaEntity extends BaseJpaEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "option_summary", nullable = false, length = 255)
    private String optionSummary;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    private OrderLineJpaEntity(Long orderId, Long productId, Long skuId, String productName,
        String optionSummary, long unitPrice, int quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.skuId = skuId;
        this.productName = productName;
        this.optionSummary = optionSummary;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public static OrderLineJpaEntity fromDomain(Long orderId, OrderLine line) {
        return new OrderLineJpaEntity(
            orderId, line.getProductId(), line.getSkuId(), line.getProductName(),
            line.getOptionSummary(), line.getUnitPrice().amount(), line.getQuantity()
        );
    }

    public OrderLine toDomain() {
        return OrderLine.reconstitute(
            getId(), productId, skuId, productName, optionSummary, new Money(unitPrice), quantity
        );
    }
}
