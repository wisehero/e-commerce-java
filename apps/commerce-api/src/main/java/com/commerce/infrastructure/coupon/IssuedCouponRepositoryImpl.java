package com.commerce.infrastructure.coupon;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.commerce.domain.coupon.CouponStatus;
import com.commerce.domain.coupon.IssuedCoupon;
import com.commerce.domain.coupon.IssuedCouponRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;
import com.commerce.support.page.PageResult;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class IssuedCouponRepositoryImpl implements IssuedCouponRepository {

    private final IssuedCouponJpaRepository issuedCouponJpaRepository;

    @Override
    public IssuedCoupon save(IssuedCoupon issuedCoupon) {
        try {
            if (issuedCoupon.getId() == null) {
                return issuedCouponJpaRepository.saveAndFlush(IssuedCouponJpaEntity.fromDomain(issuedCoupon)).toDomain();
            }
            IssuedCouponJpaEntity entity = issuedCouponJpaRepository.findById(issuedCoupon.getId())
                .orElseThrow(() -> new IllegalStateException("Issued coupon not found: " + issuedCoupon.getId()));
            entity.updateFromDomain(issuedCoupon);
            return entity.toDomain();
        } catch (DataIntegrityViolationException e) {
            throw new CoreException(ErrorType.CONFLICT, "이미 발급받은 쿠폰입니다.");
        }
    }

    @Override
    public Optional<IssuedCoupon> findById(Long id) {
        return issuedCouponJpaRepository.findById(id)
            .map(IssuedCouponJpaEntity::toDomain);
    }

    @Override
    public PageResult<IssuedCoupon> findByMemberId(Long memberId, CouponStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<IssuedCouponJpaEntity> result = status == null
            ? issuedCouponJpaRepository.findByMemberId(memberId, pageable)
            : issuedCouponJpaRepository.findByMemberIdAndStatus(memberId, status, pageable);
        return new PageResult<>(result.getContent().stream().map(IssuedCouponJpaEntity::toDomain).toList(),
            result.getTotalElements(), page, size);
    }

    @Override
    public boolean markUsedIfAvailable(Long id, Long memberId, ZonedDateTime now, Long orderId) {
        return issuedCouponJpaRepository.markUsedIfAvailable(
            id, memberId, now, orderId, CouponStatus.UNUSED, CouponStatus.USED) == 1;
    }

    @Override
    public void restoreByOrderId(Long orderId) {
        issuedCouponJpaRepository.restoreByOrderId(orderId, CouponStatus.USED, CouponStatus.UNUSED);
    }
}
