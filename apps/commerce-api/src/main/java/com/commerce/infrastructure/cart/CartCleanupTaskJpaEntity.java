package com.commerce.infrastructure.cart;

import java.time.ZonedDateTime;

import com.commerce.domain.cart.CartCleanupTask;
import com.commerce.domain.cart.CartCleanupTaskStatus;
import com.commerce.infrastructure.jpa.BaseJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart_cleanup_tasks",
    uniqueConstraints = @UniqueConstraint(name = "uk_cart_cleanup_tasks_order_id", columnNames = "order_id"),
    indexes = {
        @Index(name = "idx_cart_cleanup_tasks_ready", columnList = "status,next_attempt_at"),
        @Index(name = "idx_cart_cleanup_tasks_member_status", columnList = "member_id,status")
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartCleanupTaskJpaEntity extends BaseJpaEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "cart_id", nullable = false)
    private Long cartId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CartCleanupTaskStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private ZonedDateTime nextAttemptAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "completed_at")
    private ZonedDateTime completedAt;

    private CartCleanupTaskJpaEntity(Long orderId, Long cartId, Long memberId, CartCleanupTaskStatus status,
        int attemptCount, ZonedDateTime nextAttemptAt, String lastError, ZonedDateTime completedAt) {
        this.orderId = orderId;
        this.cartId = cartId;
        this.memberId = memberId;
        this.status = status;
        this.attemptCount = attemptCount;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = lastError;
        this.completedAt = completedAt;
    }

    public static CartCleanupTaskJpaEntity fromDomain(CartCleanupTask task) {
        return new CartCleanupTaskJpaEntity(task.getOrderId(), task.getCartId(), task.getMemberId(), task.getStatus(),
            task.getAttemptCount(), task.getNextAttemptAt(), task.getLastError(), task.getCompletedAt());
    }

    public void updateFromDomain(CartCleanupTask task) {
        status = task.getStatus();
        attemptCount = task.getAttemptCount();
        nextAttemptAt = task.getNextAttemptAt();
        lastError = task.getLastError();
        completedAt = task.getCompletedAt();
    }

    public CartCleanupTask toDomain() {
        return CartCleanupTask.reconstitute(getId(), orderId, cartId, memberId, status, attemptCount, nextAttemptAt,
            lastError, completedAt);
    }
}
