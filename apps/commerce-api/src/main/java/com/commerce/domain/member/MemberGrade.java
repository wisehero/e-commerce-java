package com.commerce.domain.member;

/**
 * 회원 등급. 쿠폰의 등급별 차등 할인에서 발급 시점 기준으로 읽힌다.
 *
 * <p>현재는 운영자가 수동으로 정하는 정적 값이다. 누적 구매액 등으로 자동 산정하지 않는다.
 * 쿠폰 등급 override는 이 값을 키로 조회하므로 등급 간 서열(높낮이) 비교는 두지 않는다.
 */
public enum MemberGrade {
    BRONZE,
    SILVER,
    GOLD,
    VIP
}
