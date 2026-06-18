package com.commerce.infrastructure.product;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.commerce.domain.brand.BrandStatus;
import com.commerce.domain.product.ProductStatus;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, Long> {

    @Query(value = """
        SELECT p FROM ProductJpaEntity p
        JOIN BrandJpaEntity b ON b.id = p.brandId
        WHERE p.status = :status
          AND b.status = :brandStatus
          AND (:keyword IS NULL OR p.name LIKE CONCAT('%', :keyword, '%'))
          AND (:categoryIds IS NULL OR p.categoryId IN (:categoryIds))
          AND (:brandId IS NULL OR p.brandId = :brandId)
        """, countQuery = """
        SELECT COUNT(p) FROM ProductJpaEntity p
        JOIN BrandJpaEntity b ON b.id = p.brandId
        WHERE p.status = :status
          AND b.status = :brandStatus
          AND (:keyword IS NULL OR p.name LIKE CONCAT('%', :keyword, '%'))
          AND (:categoryIds IS NULL OR p.categoryId IN (:categoryIds))
          AND (:brandId IS NULL OR p.brandId = :brandId)
        """)
    Page<ProductJpaEntity> search(@Param("status") ProductStatus status,
        @Param("brandStatus") BrandStatus brandStatus,
        @Param("keyword") String keyword,
        @Param("categoryIds") List<Long> categoryIds,
        @Param("brandId") Long brandId,
        Pageable pageable);


}
