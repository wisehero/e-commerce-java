package com.commerce.domain.order;

import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.Getter;

/**
 * 주문 라인. Order Aggregate 내부의 자식 엔티티(고유 id 보유).
 *
 * <p>주문 시점의 사실을 박제한다 — 상품명·옵션·단가(주문 시 salePrice)를 스냅샷해
 * 이후 상품이 변경·단종돼도 주문 내역은 구매 당시 모습을 그대로 보여준다.
 * skuId로 SKU(다른 Aggregate)를 ID 참조한다.
 */
@Getter
public class OrderLine {

    private Long id;
    private Long productId;
    private Long skuId;
    private String productName;
    private String optionSummary;
    private Money unitPrice;
    private int quantity;

    private OrderLine(Long id, Long productId, Long skuId, String productName,
        String optionSummary, Money unitPrice, int quantity) {
        this.id = id;
        this.productId = productId;
        this.skuId = skuId;
        this.productName = productName;
        this.optionSummary = optionSummary;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        validate();
    }

    public static OrderLine create(Long productId, Long skuId, String productName,
        String optionSummary, Money unitPrice, int quantity) {
        return new OrderLine(null, productId, skuId, productName, optionSummary, unitPrice, quantity);
    }

    public static OrderLine reconstitute(Long id, Long productId, Long skuId, String productName,
        String optionSummary, Money unitPrice, int quantity) {
        return new OrderLine(id, productId, skuId, productName, optionSummary, unitPrice, quantity);
    }

    private void validate() {
        if (productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 필수입니다.");
        }
        if (skuId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "SKU ID는 필수입니다.");
        }
        if (productName == null || productName.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 필수입니다.");
        }
        if (optionSummary == null || optionSummary.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "옵션 정보는 필수입니다.");
        }
        if (unitPrice == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 단가는 필수입니다.");
        }
        if (quantity < 1) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 수량은 1개 이상이어야 합니다.");
        }
    }

    /** 라인 금액 = 단가 × 수량 */
    public Money lineAmount() {
        return unitPrice.multiply(quantity);
    }
}
