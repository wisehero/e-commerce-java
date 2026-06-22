package com.commerce.application.coupon;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.commerce.domain.coupon.IssuedCouponRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@ExtendWith(MockitoExtension.class)
class CouponQueryUseCaseTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    private IssuedCouponRepository issuedCouponRepository;

    @InjectMocks
    private CouponQueryUseCase useCase;

    @Nested
    @DisplayName("회원별 쿠폰 목록 조회 - 페이징 검증")
    class GetByMemberPaging {

        @Test
        @DisplayName("page가 음수면 repository 호출 전에 BAD_REQUEST로 막는다")
        void should_throwBadRequest_when_negativePage() {
            assertThatThrownBy(() -> useCase.getByMember(MEMBER_ID, null, -1, 10))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);

            then(issuedCouponRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("size가 0이면 repository 호출 전에 BAD_REQUEST로 막는다")
        void should_throwBadRequest_when_zeroSize() {
            assertThatThrownBy(() -> useCase.getByMember(MEMBER_ID, null, 0, 0))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);

            then(issuedCouponRepository).shouldHaveNoInteractions();
        }
    }
}
