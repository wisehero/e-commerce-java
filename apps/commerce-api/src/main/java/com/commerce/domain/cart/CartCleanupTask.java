package com.commerce.domain.cart;

import java.time.ZonedDateTime;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.Getter;

/**
 * 결제 완료 후 장바구니 정리를 재시도하기 위한 내구성 작업 레코드.
 * 비즈니스 Aggregate가 아니라 트랜잭션 사이 진행 상태를 기록하는 기술적 process record다.
 * orderId가 멱등 키이며, 실패한 작업은 다음 실행 시각 이후 다시 처리한다.
 */
@Getter
public class CartCleanupTask {

    private static final long RETRY_BASE_SECONDS = 5L;
    private static final long RETRY_MAX_SECONDS = 60L;

    private final Long id;
    private final Long orderId;
    private final Long cartId;
    private final Long memberId;
    private CartCleanupTaskStatus status;
    private int attemptCount;
    private ZonedDateTime nextAttemptAt;
    private String lastError;
    private ZonedDateTime completedAt;

    private CartCleanupTask(Long id, Long orderId, Long cartId, Long memberId, CartCleanupTaskStatus status,
        int attemptCount, ZonedDateTime nextAttemptAt, String lastError, ZonedDateTime completedAt) {
        this.id = id;
        this.orderId = orderId;
        this.cartId = cartId;
        this.memberId = memberId;
        this.status = status;
        this.attemptCount = attemptCount;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = lastError;
        this.completedAt = completedAt;
        validate();
    }

    public static CartCleanupTask pending(Long orderId, Long cartId, Long memberId, ZonedDateTime now) {
        return new CartCleanupTask(null, orderId, cartId, memberId, CartCleanupTaskStatus.PENDING,
            0, now, null, null);
    }

    public static CartCleanupTask reconstitute(Long id, Long orderId, Long cartId, Long memberId,
        CartCleanupTaskStatus status, int attemptCount, ZonedDateTime nextAttemptAt, String lastError,
        ZonedDateTime completedAt) {
        return new CartCleanupTask(id, orderId, cartId, memberId, status, attemptCount, nextAttemptAt, lastError,
            completedAt);
    }

    private void validate() {
        if (orderId == null || cartId == null || memberId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카트 정리 작업의 주문·카트·회원 ID는 필수입니다.");
        }
        if (status == null || nextAttemptAt == null || attemptCount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카트 정리 작업 상태가 올바르지 않습니다.");
        }
        if (status == CartCleanupTaskStatus.COMPLETED && completedAt == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "완료된 카트 정리 작업은 완료 시각이 필요합니다.");
        }
    }

    public boolean isPending() {
        return status == CartCleanupTaskStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == CartCleanupTaskStatus.COMPLETED;
    }

    public void complete(ZonedDateTime now) {
        if (isCompleted()) {
            return;
        }
        status = CartCleanupTaskStatus.COMPLETED;
        completedAt = now;
        nextAttemptAt = now;
        lastError = null;
    }

    public void recordFailure(ZonedDateTime now, String errorMessage) {
        if (isCompleted()) {
            return;
        }
        attemptCount++;
        long retrySeconds = Math.min(RETRY_MAX_SECONDS, RETRY_BASE_SECONDS * attemptCount);
        nextAttemptAt = now.plusSeconds(retrySeconds);
        lastError = errorMessage;
    }
}
