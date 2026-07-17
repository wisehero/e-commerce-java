package com.commerce.infrastructure.cart;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface CartLineJpaRepository extends JpaRepository<CartLineJpaEntity, Long> {

    List<CartLineJpaEntity> findByCartId(Long cartId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select line from CartLineJpaEntity line where line.cartId = :cartId")
    List<CartLineJpaEntity> findByCartIdForUpdate(@Param("cartId") Long cartId);

    @Modifying
    @Query("delete from CartLineJpaEntity line where line.cartId = :cartId")
    int deleteByCartId(@Param("cartId") Long cartId);
}
