package com.commerce.application.brand;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.brand.Brand;
import com.commerce.domain.brand.BrandRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrandRegisterUseCase {

    private final BrandRepository brandRepository;

    @Transactional
    public BrandInfo register(BrandRegisterCommand command) {
        String name = normalizeName(command.name());
        if (brandRepository.existsByName(name)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 존재하는 브랜드명입니다.");
        }

        Brand brand = Brand.register(name, command.logoUrl());
        return BrandInfo.from(brandRepository.save(brand));
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim();
    }
}
