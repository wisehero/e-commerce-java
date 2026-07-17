package com.commerce.infrastructure.cart;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.commerce.domain.cart.CartCleanupTask;
import com.commerce.domain.cart.CartCleanupTaskRepository;
import com.commerce.domain.cart.CartCleanupTaskStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CartCleanupTaskRepositoryImpl implements CartCleanupTaskRepository {

    private final CartCleanupTaskJpaRepository jpaRepository;

    @Override
    public CartCleanupTask save(CartCleanupTask task) {
        if (task.getId() == null) {
            return jpaRepository.save(CartCleanupTaskJpaEntity.fromDomain(task)).toDomain();
        }
        CartCleanupTaskJpaEntity entity = jpaRepository.findById(task.getId())
            .orElseThrow(() -> new IllegalStateException("Cart cleanup task not found: " + task.getId()));
        entity.updateFromDomain(task);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<CartCleanupTask> findByOrderId(Long orderId) {
        return jpaRepository.findByOrderId(orderId).map(CartCleanupTaskJpaEntity::toDomain);
    }

    @Override
    public Optional<CartCleanupTask> findByIdForUpdate(Long taskId) {
        return jpaRepository.findByIdForUpdate(taskId).map(CartCleanupTaskJpaEntity::toDomain);
    }

    @Override
    public List<CartCleanupTask> findReady(ZonedDateTime now) {
        return jpaRepository.findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(
                CartCleanupTaskStatus.PENDING, now).stream()
            .map(CartCleanupTaskJpaEntity::toDomain)
            .toList();
    }

    @Override
    public List<CartCleanupTask> findPendingByMemberId(Long memberId) {
        return jpaRepository.findByMemberIdAndStatusOrderByIdAsc(memberId, CartCleanupTaskStatus.PENDING).stream()
            .map(CartCleanupTaskJpaEntity::toDomain)
            .toList();
    }

    @Override
    public boolean existsPendingByMemberId(Long memberId) {
        return jpaRepository.existsByMemberIdAndStatus(memberId, CartCleanupTaskStatus.PENDING);
    }
}
