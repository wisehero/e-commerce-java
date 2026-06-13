package com.commerce.application.product;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.brand.Brand;
import com.commerce.domain.brand.BrandRepository;
import com.commerce.domain.product.Product;
import com.commerce.domain.product.ProductRepository;
import com.commerce.domain.product.Sku;
import com.commerce.domain.product.SkuRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductDetailQueryUseCase {

    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;
    private final BrandRepository brandRepository;

    @Transactional(readOnly = true)
    public ProductDetailInfo getDetail(Long productId) {
        Product product = productRepository.findById(productId)
            .filter(Product::isVisible)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다."));
        Brand brand = brandRepository.findById(product.getBrandId())
            .filter(Brand::isVisible)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다."));

        List<Sku> skus = skuRepository.findByProductId(productId);

        return ProductDetailInfo.from(product, brand, skus);
    }
}
