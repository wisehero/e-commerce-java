package com.commerce.application.cart;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.commerce.domain.cart.Cart;
import com.commerce.domain.cart.CartLine;
import com.commerce.domain.product.OptionValue;
import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductRepository;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;

import lombok.RequiredArgsConstructor;

/**
 * Cart(저장값)를 live 조회로 enrich해 CartInfo로 조립한다.
 * 라인의 skuId로 Sku·Product를 <b>배치 조회</b>(findByIds)해 N+1을 피한다.
 * 조회·변경 UseCase가 공유한다(UseCase→UseCase 호출이 아닌 application 헬퍼).
 */
@Component
@RequiredArgsConstructor
public class CartInfoAssembler {

    private final SkuRepository skuRepository;
    private final ProductRepository productRepository;

    public CartInfo assemble(Cart cart) {
        List<CartLine> lines = cart.getLines();
        if (lines.isEmpty()) {
            return new CartInfo(cart.getMemberId(), List.of(), 0L);
        }

        List<Long> skuIds = lines.stream().map(CartLine::getSkuId).toList();
        Map<Long, Sku> skuById = skuRepository.findByIds(skuIds).stream()
            .collect(Collectors.toMap(Sku::getId, Function.identity()));

        List<Long> productIds = skuById.values().stream()
            .map(Sku::getProductId)
            .distinct()
            .toList();
        Map<Long, Product> productById = productRepository.findByIds(productIds).stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<CartLineInfo> lineInfos = lines.stream()
            .map(line -> toLineInfo(line, skuById, productById))
            .toList();

        long cartTotal = lineInfos.stream()
            .filter(info -> info.status() == CartLineStatus.PURCHASABLE)
            .mapToLong(CartLineInfo::lineSubtotal)
            .sum();

        return new CartInfo(cart.getMemberId(), lineInfos, cartTotal);
    }

    private CartLineInfo toLineInfo(CartLine line, Map<Long, Sku> skuById, Map<Long, Product> productById) {
        Sku sku = skuById.get(line.getSkuId());
        if (sku == null) {
            // SKU가 사라진 엣지 케이스 — 표시만 하고 구매 불가로 둔다.
            return new CartLineInfo(line.getSkuId(), line.getQuantity(), null, null, 0L, 0L, CartLineStatus.UNAVAILABLE);
        }

        Product product = productById.get(sku.getProductId());
        long salePrice = sku.getSalePrice().amount();
        long subtotal = salePrice * line.getQuantity();
        String productName = (product == null) ? null : product.getName();
        CartLineStatus status = resolveStatus(product, sku, line.getQuantity());

        return new CartLineInfo(
            line.getSkuId(),
            line.getQuantity(),
            productName,
            summarize(sku.getOptionValues()),
            salePrice,
            subtotal,
            status
        );
    }

    private CartLineStatus resolveStatus(Product product, Sku sku, int quantity) {
        if (product == null || !product.isVisible()) {
            return CartLineStatus.UNAVAILABLE;       // 판매중지·단종·미존재
        }
        if (sku.getStock().quantity() < quantity) {
            return CartLineStatus.OUT_OF_STOCK;      // 재고 부족(0 포함)
        }
        return CartLineStatus.PURCHASABLE;
    }

    private static String summarize(List<OptionValue> options) {
        return options.stream()
            .map(option -> option.name() + ":" + option.value())
            .collect(Collectors.joining(" / "));
    }
}
