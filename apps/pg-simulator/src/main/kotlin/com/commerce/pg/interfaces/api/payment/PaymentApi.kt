package com.commerce.pg.interfaces.api.payment

import com.commerce.pg.application.payment.PaymentApplicationService
import com.commerce.pg.interfaces.api.ApiResponse
import com.commerce.pg.domain.user.UserInfo
import com.commerce.pg.support.error.CoreException
import com.commerce.pg.support.error.ErrorType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payments")
class PaymentApi(
    private val paymentApplicationService: PaymentApplicationService,
) {
    @PostMapping
    fun request(
        userInfo: UserInfo,
        @RequestBody request: PaymentDto.PaymentRequest,
    ): ApiResponse<PaymentDto.TransactionResponse> {
        request.validate()

        // 100ms ~ 500ms 지연
        Thread.sleep((100..500L).random())

        // 40% 확률로 요청 실패
        if ((1..100).random() <= 40) {
            throw CoreException(ErrorType.INTERNAL_ERROR, "현재 서버가 불안정합니다. 잠시 후 다시 시도해주세요.")
        }

        return paymentApplicationService.createTransaction(request.toCommand(userInfo.userId))
            .let { PaymentDto.TransactionResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{transactionKey}")
    fun getTransaction(
        userInfo: UserInfo,
        @PathVariable("transactionKey") transactionKey: String,
    ): ApiResponse<PaymentDto.TransactionDetailResponse> {
        return paymentApplicationService.getTransactionDetailInfo(userInfo, transactionKey)
            .let { PaymentDto.TransactionDetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    fun getTransactionsByOrder(
        userInfo: UserInfo,
        @RequestParam("orderId", required = false) orderId: String,
    ): ApiResponse<PaymentDto.OrderResponse> {
        return paymentApplicationService.findTransactionsByOrderId(userInfo, orderId)
            .let { PaymentDto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
