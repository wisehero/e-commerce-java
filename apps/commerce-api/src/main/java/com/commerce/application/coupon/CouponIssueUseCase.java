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

/**
 * 쿠폰 발급 유스케이스. 한 트랜잭션에서 발급 쿠폰 저장 + 정책 발급 수량 차감을 함께 한다.
 * 정원(발급 한도) 보증은 조건부 원자 UPDATE가 맡고, 소진이면 저장까지 롤백한다.
 */
@Service
@RequiredArgsConstructor
public class CouponIssueUseCase {

    private final CouponPolicyRepository couponPolicyRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public CouponInfo issue(CouponIssueCommand command) {
        // 발급 기간 판정·만료일·응답을 한 시각 기준으로 맞춘다
        ZonedDateTime now = ZonedDateTime.now();

        // 발급 대상 적재 — 정책과 회원(등급은 할인 규칙 해석에 쓴다)
        CouponPolicy policy = couponPolicyRepository.findById(command.policyId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 쿠폰 정책입니다."));
        Member member = memberRepository.findById(command.memberId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 회원입니다."));

        // 도메인 발급(정책 규칙 검증 + 등급별 할인 해석) 후 저장
        IssuedCoupon coupon = IssuedCoupon.issue(policy, command.memberId(), member.getGrade(), now);
        IssuedCoupon saved = issuedCouponRepository.save(coupon);

        // 초과 발급 차단 — 정원 보증은 조건부 원자 UPDATE로만. 소진이면 위 저장까지 롤백
        if (!couponPolicyRepository.increaseIssuedCountIfAvailable(policy.getId())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰이 모두 소진되었습니다.");
        }

        return CouponInfo.from(saved, now);
    }
}
