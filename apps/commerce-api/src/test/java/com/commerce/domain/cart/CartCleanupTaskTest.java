package com.commerce.domain.cart;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CartCleanupTaskTest {

    private static final ZonedDateTime NOW = ZonedDateTime.parse("2026-07-17T10:00:00+09:00[Asia/Seoul]");

    @Test
    @DisplayName("새 작업은 즉시 실행 가능한 PENDING 상태다")
    void should_createPendingTask_when_enqueue() {
        // when
        CartCleanupTask task = CartCleanupTask.pending(100L, 10L, 1L, NOW);

        // then
        assertThat(task.isPending()).isTrue();
        assertThat(task.getAttemptCount()).isZero();
        assertThat(task.getNextAttemptAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("실패를 기록하면 시도 횟수를 늘리고 다음 실행 시각을 미룬다")
    void should_scheduleRetry_when_failureRecorded() {
        // given
        CartCleanupTask task = CartCleanupTask.pending(100L, 10L, 1L, NOW);

        // when
        task.recordFailure(NOW, "temporary failure");

        // then
        assertThat(task.getAttemptCount()).isEqualTo(1);
        assertThat(task.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(5));
        assertThat(task.getLastError()).isEqualTo("temporary failure");
    }

    @Test
    @DisplayName("완료 처리는 멱등하며 실패 정보를 제거한다")
    void should_completeIdempotently_when_repeated() {
        // given
        CartCleanupTask task = CartCleanupTask.pending(100L, 10L, 1L, NOW);
        task.recordFailure(NOW, "temporary failure");

        // when
        task.complete(NOW.plusMinutes(1));
        task.complete(NOW.plusMinutes(2));

        // then
        assertThat(task.isCompleted()).isTrue();
        assertThat(task.getCompletedAt()).isEqualTo(NOW.plusMinutes(1));
        assertThat(task.getLastError()).isNull();
    }
}
