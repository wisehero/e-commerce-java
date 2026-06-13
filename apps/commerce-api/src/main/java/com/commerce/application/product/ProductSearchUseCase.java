package com.commerce.application.product;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductRepository;
import com.commerce.domain.product.ProductSearchCondition;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.support.page.PageResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductSearchUseCase {

    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;

    @Transactional(readOnly = true)
    public PageResult<ProductSummaryInfo> search(String keyword, Long categoryId, Long brandId, int page, int size) {
        ProductSearchCondition condition = new ProductSearchCondition(keyword, categoryId, brandId, page, size);
        PageResult<Product> productPage = productRepository.search(condition);

        List<Long> productIds = productPage.items().stream()
            .map(Product::getId)
            .toList();

        Map<Long, Long> lowestPriceByProductId = skuRepository.findByProductIds(productIds).stream()
            .collect(Collectors.toMap(
                Sku::getProductId,
                sku -> sku.getSalePrice().amount(),
                Long::min));                       // 같은 상품의 여러 SKU → 최저가로 병합

        List<ProductSummaryInfo> items = productPage.items().stream()
            .map(p -> ProductSummaryInfo.of(
                p,
                lowestPriceByProductId.getOrDefault(p.getId(), 0L)
            ))
            .toList();

        return new PageResult<>(items, productPage.totalCount(), productPage.page(), productPage.size());
    }
}
