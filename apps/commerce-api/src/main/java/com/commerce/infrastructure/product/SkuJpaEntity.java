package com.commerce.infrastructure.product;

import java.util.List;

import com.commerce.domain.product.OptionValue;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.Stock;
import com.commerce.domain.shared.Money;
import com.commerce.infrastructure.jpa.BaseJpaEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;

@Entity
@Table(name = "skus", indexes = {
    @Index(name = "idx_skus_product_id", columnList = "product_id")
})
@Getter
public class SkuJpaEntity extends BaseJpaEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @ElementCollection
    @CollectionTable(name = "sku_option_values", joinColumns = @JoinColumn(name = "sku_id"))
    private List<OptionValueEmbeddable> optionValues;

    @Column(name = "original_price", nullable = false)
    private long originalPrice;

    @Column(name = "sale_price", nullable = false)
    private long salePrice;

    @Column(name = "stock", nullable = false)
    private int stock;

    /** 낙관적 락(OptimisticStockDeducter)용. Hibernate가 관리한다. */
    @Version
    @Column(name = "version")
    private Long version;

    protected SkuJpaEntity() {
    }

    private SkuJpaEntity(Long productId, List<OptionValueEmbeddable> optionValues,
        long originalPrice, long salePrice, int stock) {
        this.productId = productId;
        this.optionValues = optionValues;
        this.originalPrice = originalPrice;
        this.salePrice = salePrice;
        this.stock = stock;
    }

    public static SkuJpaEntity fromDomain(Sku sku) {
        List<OptionValueEmbeddable> options = sku.getOptionValues().stream()
            .map(OptionValueEmbeddable::fromDomain)
            .toList();
        return new SkuJpaEntity(
            sku.getProductId(), options,
            sku.getOriginalPrice().amount(), sku.getSalePrice().amount(), sku.getStock().quantity()
        );
    }

    public Sku toDomain() {
        List<OptionValue> options = optionValues.stream()
            .map(OptionValueEmbeddable::toDomain)
            .toList();
        return Sku.reconstitute(
            getId(), productId, options,
            new Money(originalPrice), new Money(salePrice), new Stock(stock)
        );
    }

    public void updateFromDomain(Sku sku) {
        // 현 스코프의 SKU 변경은 가격·재고뿐. productId·옵션은 불변이라 건드리지 않는다.
        this.originalPrice = sku.getOriginalPrice().amount();
        this.salePrice = sku.getSalePrice().amount();
        this.stock = sku.getStock().quantity();
    }
}
