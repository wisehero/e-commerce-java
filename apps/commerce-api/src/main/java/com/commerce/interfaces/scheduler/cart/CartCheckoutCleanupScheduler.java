package com.commerce.interfaces.scheduler.cart;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.commerce.application.cart.CartCheckoutCleanupUseCase;

import lombok.RequiredArgsConstructor;

/** MySQL에 남아 있는 PENDING 카트 정리 작업을 주기적으로 재시도한다. */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class CartCheckoutCleanupScheduler {

    private final CartCheckoutCleanupUseCase cleanupUseCase;

    @Scheduled(initialDelay = 5_000L, fixedDelay = 5_000L)
    public void retryPendingTasks() {
        cleanupUseCase.retryReady();
    }
}
