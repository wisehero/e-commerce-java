package com.commerce.domain.cart;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/** 결제 완료와 함께 기록하고 재시도하는 기술적 process record 저장 포트. */
public interface CartCleanupTaskRepository {

    CartCleanupTask save(CartCleanupTask task);

    Optional<CartCleanupTask> findByOrderId(Long orderId);

    Optional<CartCleanupTask> findByIdForUpdate(Long taskId);

    List<CartCleanupTask> findReady(ZonedDateTime now);

    List<CartCleanupTask> findPendingByMemberId(Long memberId);

    boolean existsPendingByMemberId(Long memberId);
}
