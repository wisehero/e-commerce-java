package com.commerce.application.coupon;

import java.time.ZonedDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce.domain.coupon.CouponPolicy;
import com.commerce.domain.coupon.CouponPolicyRepository;
import com.commerce.domain.coupon.IssuedCoupon;
import com.commerce.domain.coupon.IssuedCouponRepository;
import com.commerce.domain.member.Member;
import com.commerce.domain.member.MemberRepository;
import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponIssueUseCase {

    private final CouponPolicyRepository couponPolicyRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public CouponInfo issue(CouponIssueCommand command) {
        ZonedDateTime now = ZonedDateTime.now();
        CouponPolicy policy = couponPolicyRepository.findById(command.policyId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 쿠폰 정책입니다."));
        Member member = memberRepository.findById(command.memberId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 회원입니다."));
        IssuedCoupon coupon = IssuedCoupon.issue(policy, command.memberId(), member.getGrade(), now);
        IssuedCoupon saved = issuedCouponRepository.save(coupon);
        if (!couponPolicyRepository.increaseIssuedCountIfAvailable(policy.getId())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰이 모두 소진되었습니다.");
        }
        return CouponInfo.from(saved, now);
    }
}
