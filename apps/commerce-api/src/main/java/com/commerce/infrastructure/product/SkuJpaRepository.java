package com.commerce.infrastructure.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface SkuJpaRepository extends JpaRepository<SkuJpaEntity, Long> {

    List<SkuJpaEntity> findByProductId(Long productId);

    List<SkuJpaEntity> findByProductIdIn(List<Long> productIds);

    /** 비관적 락(PessimisticStockDeducter)용 — 차감 대상 SKU 행을 트랜잭션 종료까지 잠근다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SkuJpaEntity s WHERE s.id = :id")
    Optional<SkuJpaEntity> findByIdForUpdate(@Param("id") Long id);

    /**
     * 조건부 원자 차감(AtomicUpdateStockDeducter)용 — 재고가 충분할 때만 차감한다.
     * 도메인 모델을 우회하는 비교용 전략. 영향 행수 0이면 재고 부족(또는 미존재).
     */
    @Modifying
    @Query("UPDATE SkuJpaEntity s SET s.stock = s.stock - :quantity "
        + "WHERE s.id = :id AND s.stock >= :quantity")
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);
}
