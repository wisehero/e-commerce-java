package com.commerce.infrastructure.cart;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface CartJpaRepository extends JpaRepository<CartJpaEntity, Long> {

    Optional<CartJpaEntity> findByMemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cart from CartJpaEntity cart where cart.memberId = :memberId")
    Optional<CartJpaEntity> findByMemberIdForUpdate(@Param("memberId") Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cart from CartJpaEntity cart where cart.id = :cartId")
    Optional<CartJpaEntity> findByIdForUpdate(@Param("cartId") Long cartId);
}
