package com.commerce.infrastructure.cart;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CartJpaRepository extends JpaRepository<CartJpaEntity, Long> {

    Optional<CartJpaEntity> findByMemberId(Long memberId);
}
