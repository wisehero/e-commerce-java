package com.commerce.infrastructure.product;

import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductStatus;
import com.commerce.infrastructure.jpa.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_products_category_id", columnList = "category_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductJpaEntity extends BaseJpaEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "brand_id")
    private Long brandId;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    private ProductJpaEntity(String name, String description, Long categoryId,
        Long brandId, String imageUrl, ProductStatus status) {
        this.name = name;
        this.description = description;
        this.categoryId = categoryId;
        this.brandId = brandId;
        this.imageUrl = imageUrl;
        this.status = status;
    }

    public static ProductJpaEntity fromDomain(Product product) {
        return new ProductJpaEntity(
            product.getName(), product.getDescription(), product.getCategoryId(),
            product.getBrandId(), product.getImageUrl(), product.getStatus()
        );
    }

    public Product toDomain() {
        return Product.reconstitute(
            getId(), name, description, categoryId, brandId, imageUrl, status
        );
    }

    public void updateFromDomain(Product product) {
        this.name = product.getName();
        this.description = product.getDescription();
        this.categoryId = product.getCategoryId();
        this.brandId = product.getBrandId();
        this.imageUrl = product.getImageUrl();
        this.status = product.getStatus();
    }
}
