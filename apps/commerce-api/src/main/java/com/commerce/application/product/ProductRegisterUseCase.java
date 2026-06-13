package com.commerce.application.product;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.product.OptionValue;
import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductRepository;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.domain.product.Stock;
import com.commerce.domain.shared.Money;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductRegisterUseCase {

    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;

    @Transactional
    public ProductDetailInfo register(ProductRegisterCommand command) {
        if (command.skus() == null || command.skus().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품은 최소 1개의 옵션(SKU)을 가져야 합니다.");
        }

        Product product = Product.register(
            command.name(), command.description(), command.categoryId(),
            command.brandId(), command.imageUrl()
        );
        Product savedProduct = productRepository.save(product);

        List<Sku> skus = command.skus().stream()
            .map(s -> Sku.create(
                savedProduct.getId(),
                s.optionValues().stream()
                    .map(ov -> new OptionValue(ov.name(), ov.value()))
                    .toList(),
                new Money(s.originalPrice()),
                new Stock(s.stock())
            ))
            .toList();
        List<Sku> savedSkus = skuRepository.saveAll(skus);

        return ProductDetailInfo.from(savedProduct, savedSkus);
    }
}
