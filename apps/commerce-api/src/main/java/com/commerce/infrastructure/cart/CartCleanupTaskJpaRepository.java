package com.commerce.infrastructure.cart;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.commerce.domain.cart.CartCleanupTaskStatus;

import jakarta.persistence.LockModeType;

public interface CartCleanupTaskJpaRepository extends JpaRepository<CartCleanupTaskJpaEntity, Long> {

    Optional<CartCleanupTaskJpaEntity> findByOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select task from CartCleanupTaskJpaEntity task where task.id = :taskId")
    Optional<CartCleanupTaskJpaEntity> findByIdForUpdate(@Param("taskId") Long taskId);

    List<CartCleanupTaskJpaEntity> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(
        CartCleanupTaskStatus status, ZonedDateTime now);

    List<CartCleanupTaskJpaEntity> findByMemberIdAndStatusOrderByIdAsc(Long memberId,
        CartCleanupTaskStatus status);

    boolean existsByMemberIdAndStatus(Long memberId, CartCleanupTaskStatus status);
}
