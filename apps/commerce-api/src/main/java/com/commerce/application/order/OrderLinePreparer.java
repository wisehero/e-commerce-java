package com.commerce.application.order;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.commerce.application.purchase.PurchasableItem;
import com.commerce.application.purchase.PurchasableItemResolver;
import com.commerce.domain.coupon.DiscountableLine;
import com.commerce.domain.order.OrderLine;
import com.commerce.domain.product.Product;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.StockDeducter;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

/**
 * 주문 라인 준비 담당. 구매 가능성(SKU·상품·브랜드 조회·상태 검증)은
 * {@link PurchasableItemResolver}에 위임해 장바구니와 같은 기준을 쓴다. 이 객체는 주문 시점
 * 스냅샷인 OrderLine과 쿠폰 계산용 DiscountableLine을 만들고, 선택된 전략으로 재고를 차감한다.
 *
 * <p>재고 차감 전략은 외부 요청의 lockMode로 고른다(의도된 설계). 전략 선택은
 * 트랜잭션을 열기 전에 끝낼 수 있도록 resolveStrategy로 분리해 노출한다.
 * 재고 충분 여부는 StockDeducter.deduct가 차감 시점에 판정하므로 사전 검증하지 않는다.
 */
@Component
public class OrderLinePreparer {

    private final PurchasableItemResolver purchasableItemResolver;
    private final Map<String, StockDeducter> stockDeducters;

    public OrderLinePreparer(PurchasableItemResolver purchasableItemResolver,
        Map<String, StockDeducter> stockDeducters) {
        this.purchasableItemResolver = purchasableItemResolver;
        this.stockDeducters = stockDeducters;
    }

    /** lockMode에 해당하는 재고 차감 전략을 고른다. 없으면 BAD_REQUEST. */
    public StockDeducter resolveStrategy(String lockMode) {
        StockDeducter deducter = stockDeducters.get(lockMode);
        if (deducter == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 재고 차감 방식입니다: " + lockMode);
        }
        return deducter;
    }

    /** 명령의 각 라인을 검증·스냅샷하고 재고를 차감해 PreparedLine 목록으로 만든다. */
    public List<PreparedLine> prepare(OrderPlaceCommand command, StockDeducter stockDeducter) {
        List<PreparedLine> prepared = new ArrayList<>();
        for (OrderPlaceCommand.LineCommand lineCommand : command.lines()) {
            prepared.add(prepareLine(lineCommand, stockDeducter));
        }
        return prepared;
    }

    private PreparedLine prepareLine(OrderPlaceCommand.LineCommand lineCommand, StockDeducter stockDeducter) {
        PurchasableItem item = purchasableItemResolver.resolve(lineCommand.skuId());
        Sku sku = item.sku();
        Product product = item.product();

        OrderLine orderLine = OrderLine.create(
            product.getId(), sku.getId(), product.getName(),
            sku.optionSummary(), sku.getSalePrice(), lineCommand.quantity()
        );
        stockDeducter.deduct(sku.getId(), lineCommand.quantity());

        // 쿠폰 scope 매칭용 중립 입력. 박제하지 않고 이 트랜잭션에서만 쓰고 버린다.
        DiscountableLine discountableLine = new DiscountableLine(
            orderLine.lineAmount(), product.getId(), product.getBrandId(), product.getCategoryId());
        return new PreparedLine(orderLine, discountableLine);
    }
}
