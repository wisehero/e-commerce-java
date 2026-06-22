package com.commerce.application.order;

import com.commerce.domain.coupon.DiscountableLine;
import com.commerce.domain.order.OrderLine;

/** 주문 라인 준비 결과: 영속용 OrderLine과 쿠폰 계산용 DiscountableLine 한 쌍. */
record PreparedLine(OrderLine orderLine, DiscountableLine discountableLine) {
}
