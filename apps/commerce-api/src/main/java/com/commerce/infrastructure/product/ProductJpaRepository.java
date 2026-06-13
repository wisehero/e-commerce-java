package com.commerce.infrastructure.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.commerce.domain.product.ProductStatus;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, Long> {

    @Query("""
        SELECT p FROM ProductJpaEntity p
        WHERE p.status = :status
          AND (:keyword IS NULL OR p.name LIKE CONCAT('%', :keyword, '%'))
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
        """)
    Page<ProductJpaEntity> search(@Param("status") ProductStatus status,
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        Pageable pageable);


}
