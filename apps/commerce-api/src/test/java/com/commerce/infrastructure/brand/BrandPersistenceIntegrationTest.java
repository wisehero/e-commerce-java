package com.commerce.infrastructure.brand;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.commerce.domain.brand.Brand;
import com.commerce.domain.brand.BrandRepository;
import com.commerce.domain.brand.BrandStatus;
import com.commerce.support.IntegrationTestSupport;

class BrandPersistenceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @BeforeEach
    void setUp() {
        txTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void tearDown() {
        brandJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("브랜드를 저장하고 다시 도메인으로 복원한다")
    void should_persistAndReassemble_when_save() {
        // when
        Brand saved = brandRepository.save(Brand.register("나이키", "logo.jpg"));

        // then
        Brand found = brandRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("나이키");
        assertThat(found.getLogoUrl()).isEqualTo("logo.jpg");
        assertThat(found.getStatus()).isEqualTo(BrandStatus.ACTIVE);
    }

    @Test
    @DisplayName("상태 변경이 영속화된다")
    void should_persistStatusChange_when_deactivate() {
        // given
        Brand saved = brandRepository.save(Brand.register("나이키", "logo.jpg"));

        // when
        txTemplate.executeWithoutResult(s -> {
            Brand brand = brandRepository.findById(saved.getId()).orElseThrow();
            brand.deactivate();
            brandRepository.save(brand);
        });

        // then
        Brand found = brandRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(BrandStatus.INACTIVE);
    }
}
