package com.commerce.domain.order;

/** 결제 결과. 성공 여부에 따라 application이 markPaid / cancel(+재고복원)을 결정한다. */
public record PaymentResult(boolean approved) {

    public static PaymentResult success() {
        return new PaymentResult(true);
    }

    public static PaymentResult failure() {
        return new PaymentResult(false);
    }
}
