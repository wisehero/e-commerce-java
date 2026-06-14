package com.commerce.application.order;

import org.springframework.stereotype.Service;

import com.commerce.domain.coupon.IssuedCouponRepository;
import com.commerce.domain.order.Order;
import com.commerce.domain.order.OrderLine;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderCompensationHelper {

    private final SkuRepository skuRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    public void restore(Order order) {
        restoreStock(order);
        restoreCoupon(order);
    }

    private void restoreStock(Order order) {
        for (OrderLine line : order.getOrderLines()) {
            Sku sku = skuRepository.findById(line.getSkuId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 SKU입니다."));
            sku.restock(line.getQuantity());
            skuRepository.save(sku);
        }
    }

    private void restoreCoupon(Order order) {
        if (order.getUsedCouponId() != null) {
            issuedCouponRepository.restoreByOrderId(order.getId());
        }
    }
}
