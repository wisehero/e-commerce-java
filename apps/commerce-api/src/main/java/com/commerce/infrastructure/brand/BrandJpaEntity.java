package com.commerce.infrastructure.brand;

import com.commerce.domain.brand.Brand;
import com.commerce.domain.brand.BrandStatus;
import com.commerce.infrastructure.jpa.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "brands",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_brands_name", columnNames = "name")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrandJpaEntity extends BaseJpaEntity {

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BrandStatus status;

    private BrandJpaEntity(String name, String logoUrl, BrandStatus status) {
        this.name = name;
        this.logoUrl = logoUrl;
        this.status = status;
    }

    public static BrandJpaEntity fromDomain(Brand brand) {
        return new BrandJpaEntity(
            brand.getName(),
            brand.getLogoUrl(),
            brand.getStatus()
        );
    }

    public Brand toDomain() {
        return Brand.reconstitute(getId(), name, logoUrl, status);
    }

    public void updateFromDomain(Brand brand) {
        this.name = brand.getName();
        this.logoUrl = brand.getLogoUrl();
        this.status = brand.getStatus();
    }
}
