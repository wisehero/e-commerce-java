# 쿠폰(Coupon) 도메인 스펙 v1

작성: 2026-06-14 · 상태: **구현 완료(이번 이터레이션 범위)**. grill 합의 완료(§0.1 원장 #1~#27).

주문 다음으로 만드는 도메인. 본 문서는 **결정의 기록**이다. 무엇을 골랐는지가 아니라 *무엇 대신, 왜* 골랐는지를 남긴다.
이 도메인은 [`order` 스펙](../order/order-domain-spec.md)이 "주문 총액 = 라인 합 = 청구액"으로 닫아둔 금액 모델을 할인 도입으로 분기시키고(§8), 2단계 결제 흐름(order §4)에 쿠폰 소비/복원을 끼워 넣는다(§5·§6).

> **읽는 법.** 본문의 모든 결정은 두 갈래 중 하나다.
> **[확정]** = grill 세션에서 합의됨(§0.1 원장에 대안·근거와 함께 등재). **[추정]** = 내가 스펙화하며 채운 설계로, 합의된 적 없음 → 구현 전 확정 필요(§0.2).
> 본문에서 `[추정]` 태그가 붙은 것은 §0.2 소관이다. 태그 없는 서술은 [확정] 원장을 구조로 풀어쓴 것이다.

## 0. 범위

이번 이터레이션: **쿠폰 정책 생성 + 선착순 발급 + 주문 시 1장 적용(주문 단위 할인) + 결제 실패·주문 취소 시 복원**.

- 할인 적용 단위는 주문 전체, 할인 종류는 정액+정률, 발급은 회원 선착순 다운로드.
- 보류(의식적, §15): 상품/브랜드 한정 쿠폰, 스택킹, 쿠폰 사용 멱등성, 부분취소 시 부분복원, Redis 선착순, 인증 강제 적용.
- **문서 경계**: 이 스펙은 구조·구현(무엇을·어디서·어떻게)만 다룬다. 비즈니스 당위(왜 이 할인율·왜 이 만료·왜 1인 1매)는 §0.1 원장 `근거(왜)` 열과 별도 정책 문서 소관이며, 본문에서 되풀이하지 않는다.

### 0.1 결정 원장 [확정] — grill 세션 합의

각 행은 합의된 결정 + 버린 대안 + 근거다. 본문은 이 원장을 구조로 풀어쓸 뿐, 새 결정을 추가하지 않는다.

| # | 주제 | 선택 | 버린 대안 | 근거(왜) |
|---|---|---|---|---|
| 1 | 할인 적용 단위 | **주문 전체** | 상품/라인 단위, 둘 다 | 라인 매칭·할인 배분 복잡도 회피, 기존 Order를 거의 건드리지 않음 |
| 2 | 할인 종류 | **정액 + 정률** | 정액만, 정률만 | 실무 표준. 정률이 cap·반올림 invariant를 끌고 와 결정거리가 늘어남 |
| 3 | Aggregate 수 | **2개 분리** | 단일 Aggregate | 규칙+quota(공유, 1) vs 인스턴스+상태(회원별 다수)는 생명주기·동시성 상이. 단일은 quota 관리·규칙 중복 |
| 4 | 네이밍 | **CouponPolicy + IssuedCoupon** | Coupon+IssuedCoupon, CouponPolicy+Coupon, CouponCampaign+Coupon | 둘 다 수식어 → 맨이름 "쿠폰" 과적재(관리자↔고객)를 코드에서 만나지 않음. IssuedCoupon이 핵심 동사 "발급"과 직결 |
| 5 | 규칙 보유 | **발급 시 스냅샷** | policyId 참조 후 사용 시 로드 | 사용이 정책 Aggregate 비결합(주문 Txn 최소화) + 발급 후 정책 변경에 불변(공정성) |
| 6 | 소비 시점 | **재고와 동일**(Txn1 USED, Txn2 복원) | 예약→확정(RESERVED), 결제 후 사용 | 기존 재고 차감/복원과 대칭. 결제 후는 Txn1~결제 사이 중복사용 창 |
| 7 | 중복 사용 차단 | **조건부 원자 UPDATE** | 낙관락(@Version), 비관락(FOR UPDATE) | 단일행·단일사용에 락 없이 정확. 재고 원자차감(order §6)과 같은 결 |
| 8 | 만료 표현 | **파생**(저장 안 함) | 저장 상태 + 배치 전환 | 스케줄러 부재. 상태만 믿으면 만료창 누수, 어차피 날짜검사 필요(이중안전) |
| 9 | 취소 시 쿠폰 | **복원함** | 소멸(복원 안 함) | 환불+쿠폰 둘 다 반환(공정). 파생만료라 만료 분기 필요 없음 |
| 10 | 발급 방식 | **선착순 다운로드** | 관리자 직접 발급, 프로모 코드 | quota 동시성이 핵심 가치 |
| 11 | quota 동시성 | **DB 조건부 원자 UPDATE** | Redis 카운터, 비관락 | 단일 문장 강일관성으로 초과 발급 원천 차단. Redis는 DB 정합성 보정비용 |
| 12 | 1인 한도 | **1인 1매 + UNIQUE** | N매(count 검사), 무제한 | 유니크로 동시 중복발급까지 차단. 대부분 캠페인 1인 1매 |
| 13 | 만료일 산정 | **발급일 + N일**(상대) | 고정 절대기간, 둘 다(min) | "받은 날부터 N일"이 선착순 다운로드에 자연 |
| 14 | 만료 경계 | **날짜 단위·자정** | 발급시각 + N×24h | 시·분에 안 휘둘려 공정 |
| 15 | 스택킹 | **주문당 1장** | 여러 장 | 적용순서·중복할인·배분 복잡도 회피 |
| 16 | 적용 자격 범위 | **전체 주문 + 최소주문금액** | 브랜드 한정, 상품/SKU 한정 | #1과 일관(라인 매칭 회피) |
| 17 | 정률 반올림 | **버림(floor)** | 반올림, 올림 | 정수 나눗셈으로 자연스럽게 버림. 초과 할인 없음, 국내 관례 |
| 18 | 할인 > 총액 | **클램프**(청구액 ≥ 0) | 거부(에러) | UX + Money 비음수 invariant 유지 |
| 19 | 정률 cap | **선택(nullable)** | 필수 | 무제한/한도 둘 다 표현 |
| 20 | 계산·검증 위치 | **IssuedCoupon 도메인 메서드 → `DiscountRule` 위임(#24)** | 도메인 Service, UseCase | 도메인이 invariant 소유. 스냅샷이라 쿠폰이 자기 규칙을 스스로 안다 |
| 21 | 0원 결제 | **게이트웨이 스킵 → PAID** | 0원도 호출 | 0원 결제 무의미 |
| 22 | ErrorType | **기존 4종 재사용** | 쿠폰 전용 추가 | 프로젝트 현 컨벤션 일관 |
| 23 | Order 금액 | **주문 총액+할인+청구 3필드** | totalAmount 재정의, discount만 추가 | 세 금액 모두 사실 보존, 정책 §4와 정합 |
| 24 | 할인 규칙 구조 | **공유 VO `DiscountRule`** | 두 Root에 직접 | invariant·계산 한 곳, 스냅샷=VO 복사, `ddd.md §8`. #20을 'VO 위임'으로 정정 |
| 25 | minOrderAmount 위치 | **`DiscountRule`에 포함** | 분리 VO/필드 | 자격 조건이 하나뿐, YAGNI. 조건 늘면 그때 분리 |
| 26 | 정률 정밀도 | **정수 %(1~100)** | 소수(basis point) | 단순, 대부분 정수 %. 소수 필요 시 basis point 확장 |
| 27 | 정책 긴급 중단 | **`active` 플래그** | issuableUntil 수정 | 기간과 독립된 on/off, 끄고 되켜기 명확 |

### 0.2 추정 [추정] — 컨벤션 수준, 구현 시 확정

설계 판단(VO 추출·minOrderAmount 위치·정률 정밀도·긴급 중단)은 grill로 닫혀 원장 #24~#27로 옮겼다. 여기 남은 건 명칭·매핑 수준의 낮은 위험 추정뿐이다.

| 항목 | 추정안 | 위험도 | 비고 |
|---|---|---|---|
| 영속 매핑 | `DiscountRule` `@Embedded`, UNIQUE(policy_id, member_id) | 낮음 | JPA 연관 어노테이션 없음 유지 |
| API 표면 | `/api/v1/...` 경로, `CouponControllerV1` 등 | 낮음 | order(`/api/v1/orders`) 컨벤션 유도 |
| 팩토리·필드명 | `create`/`issue`/`reconstitute`, `issuableFrom/Until`, `usedOrderId`, Money 확장명 | 낮음 | order(`place`/`reconstitute`, `orderedAt`) 컨벤션 유도 |

## 1. Aggregate 구조 — 두 Aggregate 분리

[확정 #3·#4] 성격이 다른 두 개념을 별도 Aggregate Root로 나눈다.

| Aggregate | 정체 | 수량 | 동시성 경합 지점 |
|---|---|---|---|
| **`CouponPolicy`** | 캠페인/규칙: 할인 정의 + 발급 가능 기간 + 선착순 총량 | 정책당 1(공유) | 발급 시 `issuedCount` 증가(다수 회원 경합) |
| **`IssuedCoupon`** | 회원 보유 인스턴스: 규칙 스냅샷 + 상태 | 발급당 1(회원별 다수) | 사용 시 단일 행 상태 전이(동일 쿠폰 중복 사용) |

- 상대 Aggregate는 **ID로만 참조**(`ddd.md §5`): `IssuedCoupon.policyId`·`memberId`·`usedOrderId`. 연관 어노테이션 없음.
- 고객 대면 표현("내 쿠폰")은 interfaces 계층에서 `IssuedCoupon → CouponResponse`로 매핑(내부 정밀성이 밖으로 새지 않음).

### `DiscountRule` (공유 Value Object)

> [확정 #24] 할인 규칙을 공유 VO로 둔다. `CouponPolicy`가 보유하고, 발급 시 `IssuedCoupon`으로 복사(스냅샷)한다. 계산 본체가 이 VO에 살고 `IssuedCoupon`이 위임한다(#20 정정).

| 필드 | 타입 | 비고 |
|---|---|---|
| type | `DiscountType` | `FIXED`(정액) / `RATE`(정률) |
| value | long | FIXED=할인 원, RATE=할인 %(정수 1~100, #26) |
| maxDiscountAmount | Money (nullable) | [확정 #19] 정률 cap. null=무제한 |
| minOrderAmount | Money | [확정 #16·#25] 적용 최소 주문 금액(VO에 포함) |

- invariant: `RATE`면 `value ∈ 1..100`, `FIXED`면 `value > 0`.
- 행위: `calculateDiscount(Money lineTotal): Money`(§7). 불변·값 동등성.

### `CouponPolicy` (Root)

| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | |
| name | String | 캠페인명 |
| discountRule | DiscountRule | [확정 #24] (위 참조) |
| validDays | int | [확정 #13] 발급된 쿠폰의 유효일수 |
| issuableFrom / issuableUntil | ZonedDateTime | 발급 가능 기간(캠페인 창). `[추정: 필드명]` |
| maxIssueCount | long | [확정 #10·#11] 선착순 총량 |
| issuedCount | long | 발급 누적. 조건부 원자 UPDATE 대상(§10) |
| active | boolean | [확정 #27] 긴급 중단 스위치 |

- invariant: `validDays > 0`, `maxIssueCount > 0`, `issuableFrom < issuableUntil`.
- 팩토리: `create(...)`(id=null, issuedCount=0) / `reconstitute(...)`. `[추정: 명]`
- 행위: `assertIssuable(now)`(발급 가능 기간 검증). `issuedCount` 증가의 **원자성은 repository 조건부 UPDATE가 보장**(§10). 도메인은 비즈니스 규칙만 책임진다.

### `IssuedCoupon` (Root)

| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | |
| policyId | Long | 발급 출처, ID 참조 |
| memberId | Long | 소유자, ID 참조 |
| discountRule | DiscountRule | [확정 #5·#24] **스냅샷**(발급 시점 규칙 복사) |
| status | CouponStatus | [확정 #6·#8] `UNUSED` / `USED`(만료는 파생) |
| issuedAt | ZonedDateTime | 발급 시각. 도메인 사실로 직접 보유 |
| expiresAt | ZonedDateTime | [확정 #13·#14] **스냅샷**(= 발급일 + validDays, §9) |
| usedOrderId | Long (nullable) | [확정 #9] 복원 가드용(§6). `[추정: 명]` |

- 팩토리: `issue(policy, memberId, now)`(스냅샷 + expiresAt 계산) / `reconstitute(...)`.
- 행위 [확정 #20]:
  - `calculateDiscount(Money lineTotal)` → `discountRule`에 위임(#24).
  - `use(memberId, lineTotal, now, orderId)` — 소유자·상태·만료·최소금액 invariant 검증 후 `UNUSED→USED` + `usedOrderId` 기록. 동시 중복 사용 차단의 **원자성은 repository 조건부 UPDATE**(§10).
  - `restore(orderId)` — `USED→UNUSED`(§6).
- `issuedAt`/`expiresAt`은 `BaseJpaEntity.createdAt`에 기대지 않는다(order `orderedAt`과 동일 정신).

## 2. 스냅샷 (발급 = 그 순간 규칙의 박제)

[확정 #5] `IssuedCoupon`은 사용 시 `CouponPolicy`를 조인하지 않아도 할인을 계산할 만큼 규칙을 자족적으로 박제한다(OrderLine 스냅샷, order §2와 동일 논리). 발급 후 정책 수정이 기발급 쿠폰에 소급되지 않고(공정성), 주문 트랜잭션이 `CouponPolicy` Aggregate를 읽지 않는다. `expiresAt`도 발급 시 절대 시각으로 박제.

## 3. 상태머신 (2상태 + 파생 만료)

```
issue()    → UNUSED                (발급)
use()      : UNUSED → USED         (주문 Txn1: 소비)
restore()  : USED   → UNUSED       (결제 실패 보상 / 주문 취소)
만료        : 저장 상태 아님 — expiresAt vs now로 파생(§9)
```

[확정 #8] 만료를 저장 상태로 두지 않는다. 스케줄러가 없고, 상태만 믿으면 "만료일 지났지만 미전환" 창에서 사용됨. 사용 가드가 항상 `expires_at > now`를 함께 본다. 따라서 만료 일괄 전환 배치가 불필요하고, "만료된 쿠폰 목록"은 조회 시 날짜로 필터한다.

## 4. 발급 흐름 (선착순)

```
Txn: CouponPolicy.assertIssuable(now)     (발급 가능 기간 검증)
     → IssuedCoupon INSERT                 (UNIQUE(policy_id, member_id) → 중복 시 CONFLICT)
     → 조건부 quota UPDATE                   (0행이면 소진 → BAD_REQUEST, Txn 롤백)
     → commit
```

- [확정 #12] **1인 1매**: `(policy_id, member_id)` UNIQUE. 동시 중복 발급도 DB가 차단.
- [확정 #10·#11] **선착순 총량**: `UPDATE coupon_policy SET issued_count = issued_count + 1 WHERE id = :id AND issued_count < max_issue_count`. 0행이면 소진, 초과 발급을 원천 차단한다.

**1 Txn 정합성**: INSERT(유니크 가드)와 quota UPDATE(소진 가드)를 한 트랜잭션에 둔다. 어느 가드가 실패하든 예외 → 전체 롤백이므로, 순서와 무관하게 부분 효과(quota만 증가)가 남지 않는다. 유니크 위반은 도메인 예외(`CONFLICT`)로 변환해 롤백시킨다.

**의식적 규칙 예외: 발급 Txn의 다중 Aggregate 수정** — `ddd.md §4`("1 Txn = 1 Aggregate")와 충돌하나 의식적 예외다(order Txn1이 SKU+Order를 묶은 것과 동일 성격). 발급은 `CouponPolicy`(카운트) + `IssuedCoupon`(생성)을 한 Txn에 묶어야 초과 발급을 막고, 외부 호출이 없어 락 점유가 짧다.

## 5. 사용 흐름 (주문 통합)

[확정 #6] `OrderPlaceCommand`에 선택 입력 `couponId` 추가. 쿠폰 소비는 재고 차감과 동일 패턴으로 order §4의 2단계 흐름에 얹는다.

```
Txn1: (기존) member·sku·product·brand 검증 + 라인 스냅샷 + 재고 차감
      → couponId 있으면: IssuedCoupon 로드 + 소유자(memberId) 검증
      → discount = issuedCoupon.calculateDiscount(라인합)
      → 조건부 쿠폰 사용 UPDATE                              (§10, 0행이면 이미사용/만료 → BAD_REQUEST)
      → Order.place(memberId, lines, discount, couponId)    (payableAmount 계산, §8)
      → Order(PAYMENT_PENDING) 저장 + commit
결제 호출   ← 트랜잭션 밖
   payableAmount == 0 이면 게이트웨이 스킵                    [확정 #21]
Txn2: 성공(또는 0원 스킵) → markPaid()
      실패 → cancel() + 재고 복원 + 쿠폰 복원(§6)
```

- 소유자 검증: `issuedCoupon.memberId == command.memberId` 불일치면 거부(단, 주문 `memberId`가 미인증이라 order의 인증 보류 수준에 종속, §15).
- [확정 #21] **0원 결제**: 전액 할인으로 `payableAmount == 0`이면 결제 단계를 건너뛰고 Txn2에서 바로 `PAID`.

## 6. 취소·복원 흐름

```
주문 취소(OrderCancelUseCase) Txn:
   cancel() + 재고 복원 + 쿠폰 복원(usedCouponId 있으면)
환불   ← 트랜잭션 밖. PAID였던 주문만, payableAmount 기준
```

- [확정 #6·#9] 결제 실패 보상(Txn2)과 유저 주문 취소 모두 쿠폰을 `USED→UNUSED`로 복원.
- 복원은 조건부 UPDATE `... SET status='UNUSED', used_order_id=NULL WHERE status='USED' AND used_order_id=:orderId` — 멱등.
- [확정 #8] 파생 만료라 만료일 지난 쿠폰을 복원해도 자동 사용 불가 → 만료 분기 필요 없음.
- 환불 금액은 `totalAmount`가 아니라 **`payableAmount`**(고객 실지불액). order §5 환불을 쿠폰 도입에 맞춰 청구액 기준으로 정정.

## 7. 할인 계산

[확정 #16·#17·#18·#19] `calculateDiscount(Money lineTotal)`:

```
0) 자격: lineTotal < minOrderAmount  → BAD_REQUEST
1) raw    = (type == FIXED) ? value : floor(lineTotal × value / 100)   // 정수 나눗셈 = 자연 floor
2) capped = (maxDiscountAmount != null) ? min(raw, cap) : raw
3) discount = min(capped, lineTotal)                                    // 총액 초과분 클램프
```

자격 기준 금액은 할인 전 라인합(주문 총액). 클램프로 청구액 최소 0, `Money` 비음수 invariant 유지.

## 8. 금액 모델 — `Order` 3필드로 분기

[확정 #23] order §7("총액 = 라인 합 = 청구액")이 쿠폰으로 주문 총액↔청구액으로 갈라진다.

| 필드 | 의미 | 비고 |
|---|---|---|
| totalAmount | 주문 총액(라인 합) | 유지. `place()`가 자체 계산 |
| discountAmount | 쿠폰 할인액 | 신설. 쿠폰 없으면 `Money.ZERO` |
| payableAmount | 청구액 = totalAmount − discountAmount | 신설. 결제·환불 기준 |
| usedCouponId | 사용한 쿠폰 | 신설, nullable. `[추정: 명]` |

- 세 금액 모두 사실 기록으로 보존. 결제 호출부 `getTotalAmount()` → **`getPayableAmount()`**.
- `[추정]` `Money` 확장: `minus(Money)`(비음수 유지), 정률 floor 헬퍼, `min(Money)`. 클램프를 §7에서 끝내므로 `payable`은 음수가 되지 않는다.
- 라인 단위 할인 배분은 안 한다(주문당 1장·전체취소만). 미래 부분취소 시 라인 분배 규칙 필요(§15).

## 9. 만료 정의

[확정 #13·#14] `expiresAt = 발급일.plusDays(validDays).atStartOfDay()`(= 더 이상 유효하지 않은 첫 순간). 유효 조건 `now < expiresAt`. 발급 당일 포함, 마지막 날 자정 만료. 시·분 무관. 사용·복원 가드의 `expires_at > :now`가 이 정의를 따른다.

## 10. 동시성 — 조건부 원자 UPDATE 3곳

[확정 #7·#10·#11] 락 없이 단일 문장 compare-and-set으로 경합을 차단한다. order의 원자 `StockDeducter`(order §6)와 동일 형태로 프로젝트에 선례가 있다.

| 지점 | 쿼리 가드 | 0행일 때 |
|---|---|---|
| 발급 quota | `WHERE id=:id AND issued_count < max_issue_count` | 소진 → BAD_REQUEST |
| 쿠폰 사용 | `WHERE id=:id AND status='UNUSED' AND expires_at > :now` | 이미사용/만료 → BAD_REQUEST |
| 쿠폰 복원 | `WHERE status='USED' AND used_order_id=:orderId` | 이미 복원됨(멱등, 무동작) |

**DDD 긴장점**: 원자성은 infrastructure의 조건부 UPDATE가 보장하고, 도메인 메서드(`assertIssuable`·`use`·`calculateDiscount`)는 검증·계산·전이 의도를 소유한다. 도메인 모델을 우회하는 UPDATE는 order §6 `AtomicUpdateStockDeducter`와 같은 비용(invariant 우회)을 지며 테스트·문서에 남긴다. 쿠폰 사용 전략은 단일 고정(재고처럼 `lockMode` 노출 안 함). 단일 행·단일 사용이라 조건부 UPDATE 하나로 충분하다.

## 11. 유스케이스 (application)

| UseCase | 입력 | 출력 | 비고 |
|---|---|---|---|
| 정책 생성(관리자) | `CouponPolicyCreateCommand` | `CouponPolicyInfo` | Txn |
| 쿠폰 발급(선착순) | `CouponIssueCommand`(policyId, memberId) | `CouponInfo` | §4 발급 Txn |
| 내 쿠폰 목록 | memberId, status?, page, size | `PageResult<CouponInfo>` | readOnly. 만료는 파생 필터 |
| (주문 생성 확장) | `OrderPlaceCommand`(+couponId) | `OrderInfo` | order UseCase에 쿠폰 소비 삽입(§5) |

명칭은 `[추정]`(order 컨벤션 유도). 신규 발급/조회는 두 Repository만, 주문 생성은 기존 `OrderPlaceUseCase`에 `IssuedCouponRepository` 의존 추가.

## 12. 5계층 매핑 `[추정: 경로·명칭]`

- **interfaces** (`com.commerce.interfaces.api.coupon`): `CouponControllerV1`(`/api/v1/coupons` 발급, `/api/v1/members/{memberId}/coupons` 내 쿠폰), `CouponPolicyControllerV1`(`/api/v1/coupon-policies`). `ApiResponse<T>` 직반환(프로젝트 컨벤션).
- **application** (`com.commerce.application.coupon`): UseCase, `*Command`/`*Info`.
- **domain** (`com.commerce.domain.coupon`): `CouponPolicy`/`IssuedCoupon`/(`DiscountRule`)/`DiscountType`/`CouponStatus`, `CouponPolicyRepository`·`IssuedCouponRepository`.
- **infrastructure** (`com.commerce.infrastructure.coupon`): `*JpaEntity`, JpaRepository(조건부 UPDATE 쿼리 포함), `RepositoryImpl`. `DiscountRule`은 `@Embedded` 검토. UNIQUE(policy_id, member_id).

## 13. ErrorType — 신규 없음

[확정 #22] 기존 4종 재사용 + 구체 메시지.

| 상황 | ErrorType |
|---|---|
| 없는 쿠폰/정책 | `NOT_FOUND` |
| 만료·최소금액 미달·소유자 불일치·소진·이미 사용 | `BAD_REQUEST` |
| 1인 중복 발급(유니크 위반) | `CONFLICT` |

프론트가 코드로 분기해야 하면("소진" vs "이미 발급" UX 구분) 쿠폰 전용 ErrorType 추가 재검토(§15).

## 14. 검증 기준

- **domain 단위**: 할인 계산(정액/정률 floor/cap/클램프/최소금액 경계), 상태 전이, 만료 경계(off-by-one), 소유자 검증.
- **동시성**(Testcontainers): 같은 정책 N 동시 발급 → `issuedCount ≤ maxIssueCount` + 1인 중복 차단. 같은 쿠폰 N 동시 사용 → 1회만 성공.
- **application**: 결제 성공/실패 목 → 쿠폰 `USED`/복원. 주문 취소 → 쿠폰 복원 + `payableAmount` 환불. 0원 주문 → 게이트웨이 미호출 + `PAID`.
- `@WebMvcTest`: 발급·내쿠폰·정책생성 + 쿠폰 적용 주문. ArchUnit `LayerDependencyTest` · Spotless 그린.

## 15. 보류·확장 지점 (의식적으로 미룸)

미결(권장 기본값만 둔 것)은 §0.2로 이동했다. 여기는 **기능 확장**만 둔다.

- **orphan/멱등성** (order §8·§14 상속): 결제 성공 후 Txn2 커밋 전 크래시 → 쿠폰 `USED`·재고 차감·주문 `PAYMENT_PENDING`·돈 빠짐. `POST /orders` 재시도 시 둘째 시도가 조건부 UPDATE 0행으로 실패. 주문 멱등키 작업과 한 묶음.
- 상품/브랜드/카테고리 한정 쿠폰(자격에 라인 매칭 재도입).
- 스택킹(주문당 N장, 적용 순서·배분).
- 부분취소 시 쿠폰 부분 복원(라인 단위 할인 배분 필요).
- 관리자 일괄 발급·프로모 코드·이벤트 자동 발급.
- 고트래픽 선착순: Redis 원자 카운터(`modules/redis` 첫 사용처, Redis와 DB 정합성 보정비용).
- 인증·인가 강제 적용(현재 소유자 검증은 `memberId` 비교 의존).
