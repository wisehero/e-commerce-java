package com.commerce.application.brand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commerce.domain.brand.Brand;
import com.commerce.domain.brand.BrandRepository;
import com.commerce.domain.brand.BrandStatus;

@ExtendWith(MockitoExtension.class)
class BrandStatusChangeUseCaseTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandStatusChangeUseCase useCase;

    @Test
    @DisplayName("브랜드를 비활성화한다")
    void should_deactivateBrand() {
        // given
        given(brandRepository.findById(1L))
            .willReturn(Optional.of(Brand.reconstitute(1L, "나이키", "logo.jpg", BrandStatus.ACTIVE)));
        given(brandRepository.save(any(Brand.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        useCase.deactivate(1L);

        // then
        ArgumentCaptor<Brand> captor = ArgumentCaptor.forClass(Brand.class);
        then(brandRepository).should().save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(BrandStatus.INACTIVE);
    }
}
