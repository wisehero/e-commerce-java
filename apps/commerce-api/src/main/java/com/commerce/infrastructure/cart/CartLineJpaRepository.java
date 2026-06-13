package com.commerce.infrastructure.cart;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CartLineJpaRepository extends JpaRepository<CartLineJpaEntity, Long> {

    List<CartLineJpaEntity> findByCartId(Long cartId);

    void deleteByCartId(Long cartId);
}
