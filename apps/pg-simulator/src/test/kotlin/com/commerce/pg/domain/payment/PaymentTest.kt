package com.commerce.pg.domain.payment

import com.commerce.pg.support.error.CoreException
import com.commerce.pg.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class PaymentTest {
    @Test
    fun `대기 중인 결제를 매입하면 CAPTURED 상태가 된다`() {
        val payment = payment()

        payment.capture()

        assertThat(payment.status).isEqualTo(TransactionStatus.CAPTURED)
        assertThat(payment.refundedAmount).isZero()
        assertThat(payment.refundableAmount).isEqualTo(5_000L)
    }

    @Test
    fun `매입된 결제를 일부 환불한 뒤 나머지를 환불할 수 있다`() {
        val payment = payment()
        payment.capture()

        payment.refund(2_000L)

        assertThat(payment.status).isEqualTo(TransactionStatus.PARTIALLY_REFUNDED)
        assertThat(payment.refundedAmount).isEqualTo(2_000L)
        assertThat(payment.refundableAmount).isEqualTo(3_000L)

        payment.refund(3_000L)

        assertThat(payment.status).isEqualTo(TransactionStatus.REFUNDED)
        assertThat(payment.refundedAmount).isEqualTo(5_000L)
        assertThat(payment.refundableAmount).isZero()
    }

    @Test
    fun `환불 가능 금액을 초과하면 거절한다`() {
        val payment = payment()
        payment.capture()

        assertThatThrownBy { payment.refund(5_001L) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.BAD_REQUEST)
    }

    @Test
    fun `매입되지 않은 결제는 환불할 수 없다`() {
        val payment = payment()

        assertThatThrownBy { payment.refund(1_000L) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.CONFLICT)
    }

    private fun payment(): Payment = Payment.create(
        transactionKey = "20260717:TR:test01",
        userId = "commerce-api",
        orderId = "000123",
        cardType = CardType.SAMSUNG,
        cardNo = "1234-5678-9814-1451",
        amount = 5_000L,
        callbackUrl = "http://localhost:8080/api/v1/payments/callback",
    )
}
