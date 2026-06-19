# 쿠폰(Coupon) 도메인 스펙 v2

작성: 2026-06-14 · v2 증분: 2026-06-18 · 상태: **v1 구현 완료**, **v2 설계 합의 완료**(§0.1 원장 #1~#27, §0.1b 원장 #28~#39).

주문 다음으로 만드는 도메인. 본 문서는 **결정의 기록**이다. 무엇을 골랐는지가 아니라 *무엇 대신, 왜* 골랐는지를 남긴다.
이 도메인은 [`order` 스펙](../order/order-domain-spec.md)이 "주문 총액 = 라인 합 = 청구액"으로 닫아둔 금액 모델을 할인 도입으로 분기시키고(§8), 2단계 결제 흐름(order §4)에 쿠폰 소비/복원을 끼워 넣는다(§5·§6).

> **v2 증분(2026-06-18).** v1은 "주문 전체 할인 + 비차등"이었다. v2는 두 직교 축을 더한다 — **적용 범위(scope)**: 할인이 *어느 라인에* 붙나(`WHOLE`/`BRAND`/`PRODUCT`/`CATEGORY`), **등급 차등**: 할인 규칙이 회원 등급에 따라 *얼마나* 센가. 두 축은 독립이라 자유 조합된다. v1 결정 중 #1·#16(주문 전체 한정)은 **반전이 아니라 확장**이다 — `WHOLE`이 기본값으로 남고 scope가 위에 얹힌다. v2 증분 결정·근거·대안은 §0.1b 원장에, 본문 갱신 지점은 각 절의 `[v2]` 태그로 표시한다.
>
> **v2 후속(2026-06-18, 카테고리 도메인 도입 이후).** 최초 v2 합의는 `CATEGORY` scope를 보류했다(#31, 당시 카테고리 도메인 부재). 이후 계층형 카테고리 도메인(`domain/category`, depth 1~3, `parentId`, `findSelfAndDescendantIds`)이 생겨 전제가 바뀌었고, `CATEGORY` scope를 **서브트리 매칭 + 주문 시점 신선 해소**로 추가했다(원장 #40~#42). #31은 갱신됨(보류 해제).

> **읽는 법.** 본문의 모든 결정은 두 갈래 중 하나다.
> **[확정]** = grill 세션에서 합의됨(§0.1 원장에 대안·근거와 함께 등재). **[추정]** = 내가 스펙화하며 채운 설계로, 합의된 적 없음 → 구현 전 확정 필요(§0.2).
> 본문에서 `[추정]` 태그가 붙은 것은 §0.2 소관이다. 태그 없는 서술은 [확정] 원장을 구조로 풀어쓴 것이다.

## 0. 범위

**v1 이터레이션**: 쿠폰 정책 생성 + 선착순 발급 + 주문 시 1장 적용(주문 단위 할인) + 결제 실패·주문 취소 시 복원.

- 할인 적용 단위는 주문 전체, 할인 종류는 정액+정률, 발급은 회원 선착순 다운로드.

**v2 이터레이션** [v2]: **적용 범위 한정 쿠폰**(`WHOLE`/`BRAND`/`PRODUCT`/`CATEGORY`) + **등급 차등 할인**(발급 시점 등급으로 규칙 확정).

- 한정 쿠폰: 할인이 scope에 매칭되는 라인 부분합에만 적용(scoped base). `CATEGORY`는 계층형 카테고리 도메인 도입 후 **서브트리 매칭 + 주문 시점 신선 해소**로 추가(#40~#42).
- 등급 차등: `CouponPolicy`가 기본 규칙 + 등급별 override 맵을 들고, 발급 시 회원 등급으로 단일 규칙을 골라 박제(#36·#38). `IssuedCoupon`·주문 계산 흐름은 v1 그대로 보존.
- **선행 의존성**: 회원 등급 개념이 member 도메인에 없었다 → `MemberGrade` 최소 정적 enum + `Member.grade` 신설(#37·#39). 자동 산정은 보류. 카테고리 한정은 `domain/category`(계층형) 도입이 선행됐다.

공통 보류(의식적, §15): 스택킹, 쿠폰 사용 멱등성, 부분취소 시 부분복원, Redis 선착순, 인증 강제 적용 / [v2] 다중·교차 대상, 등급 자동 산정, 등급 발급 게이트, 등급별 scope 차등.
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

### 0.1b 결정 원장 [확정] — grill 2회차 (2026-06-18, v2 증분)

v1 원장의 #1·#16(주문 전체 한정)을 반전하지 않고 확장한다. `WHOLE`은 기본 scope로 남는다.

| # | 주제 | 선택 | 버린 대안 | 근거(왜) |
|---|---|---|---|---|
| 28 | 쿠폰 종류 모델링 | **직교 2축 분리**(scope + 등급차등) | 단일 `CouponType` enum, 두 기능 완전 분리 | "한정=적용범위"와 "등급=할인규칙"은 서로 다른 축. 한 enum은 조합("GOLD+브랜드X") 표현 불가/폭발 |
| 29 | 한정의 의미 | **적용 범위(scoped base)** | 게이트(gate) | "브랜드X 20%"는 X 라인 부분합에만 할인이 진짜 의미. 게이트("X 있으면 전체 할인")는 운영 의도와 어긋남 |
| 30 | 대상 모양 | **단일 차원 + 단일 대상 ID** | 단일차원 다중ID, 다차원 교차(AND/OR) | YAGNI. 대부분 프로모션을 커버, 다중·교차는 규칙 엔진을 끌고 옴 |
| 31 | scope 차원 | **`WHOLE`/`BRAND`/`PRODUCT`** (~~CATEGORY 보류~~ → #40에서 추가) | CATEGORY 무검증, 최소 Category 도메인 | 결정 당시 category는 도메인·레포·관리수단 전무(자유 Long) → 검증·운영 불가라 보류. **이후 계층형 카테고리 도메인 도입으로 전제 변화 → #40에서 CATEGORY 추가** |
| 32 | scoped 계산 책임 | **`IssuedCoupon`이 중립 입력 `List<DiscountableLine>`로 계산** | UseCase가 매칭·합산, coupon이 `OrderLine` 직접 수신 | 어느 라인이 대상인가 = 할인 규칙의 일부 → 도메인이 소유. `OrderLine` 직접 수신은 Aggregate 경계 위반, 중립 레코드로 차단 |
| 33 | 라인 brand/category 박제 | **안 함, 계산 시점 전사**(transient) | `OrderLine`에 brandId/categoryId 박제 | 할인은 1회 계산 후 결과(할인액·청구액)만 `Order`에 박힘. 매칭은 UseCase가 이미 로드한 `Product`에서 즉석 조립. 감사/부분취소 필요 시 그때 박제 |
| 34 | scoped 최소주문금액 기준 | **매칭 부분합** | 주문 전체 총액 | 자격 base와 할인 base가 같아야 일관. 귀결: 매칭 라인 0 → 부분합 0 → **명시적 거부**("적용 대상 상품 없음"), 조용한 0원 할인 금지 |
| 35 | 등급 쿠폰 의미 | **등급별 혜택 차등** | 발급 게이트(등급≥X만 수령), 등급 자동 지급 | 사용자 의도가 "등급마다 할인이 다르다". 게이트는 `discountRule` 단일이라 자격 축으로 분리 가능하나 이번 의도 아님. 자동 지급은 발급 트리거 인프라 필요 |
| 36 | 등급 확정 시점 | **발급 시점**(스냅샷) | 사용 시점 | `IssuedCoupon`의 단일 규칙 박제(#5) 불변식·주문 계산 흐름 보존. 등급은 `CouponIssueUseCase`에서만 읽힘. "혜택=발급 순간 등급 고정" |
| 37 | 회원 등급 도입 | **최소 정적 enum `MemberGrade` + `Member.grade`** | 활동 기반 자동 산정(누적구매액 배치) | 푸는 문제는 쿠폰 등급 차등이지 멤버십 산정 시스템이 아님. 쿠폰은 grade 읽기만. 자동 산정은 별도 에픽 |
| 38 | 등급→규칙 커버리지 | **기본 규칙 + 등급별 override 부분 맵** | 전 등급 규칙 필수, 부분맵+폴백없음(게이트 회귀) | 하위 호환: 기존 비차등 쿠폰 = override 비어 있음. 일반 쿠폰이 맵을 강제당하지 않고, 새 등급도 기본으로 흡수. 발급 시 단일 규칙으로 해소(#36) |
| 39 | MemberGrade 값 | **`BRONZE`/`SILVER`/`GOLD`/`VIP`** | 임의/타 명칭 | override가 키 조회라 등급 서열(≥) 불필요 — 값 집합으로만 존재. 4단계는 통상 등급 체계 |

#### grill 3회차 (2026-06-18, 카테고리 도메인 도입 이후) — #31 갱신

| # | 주제 | 선택 | 버린 대안 | 근거(왜) |
|---|---|---|---|---|
| 40 | CATEGORY scope | **추가**(#31 보류 해제) | 계속 보류 | 계층형 카테고리 도메인 도입으로 #31 보류 사유(도메인·레포 부재) 소멸. `CategoryRepository.findById`로 대상 검증 가능 |
| 41 | 카테고리 매칭 | **서브트리 일치**(대상 + 모든 하위) | 평면(정확) 일치 | 상품은 리프(depth 3) 카테고리에만 매임(`ProductRegisterUseCase`의 `isLeaf()` 강제) → 비리프 평면 매칭은 0개. `findSelfAndDescendantIds`가 이미 "상위 검색 시 하위 포함" 의도로 존재. 리프 대상은 서브트리=자기자신이라 평면을 포함 |
| 42 | 서브트리 해소 시점 | **주문(사용) 시점 신선 해소** | 발급 시점 서브트리 박제 | 카테고리 멤버십은 "무엇이 그 카테고리에 속하나"라는 구조적 사실 → 사용 시점 현재 상태가 옳음. 발급 후 추가된 하위·상품도 포함. 할인규칙 박제(#36)와는 다른 관심사라 비모순. `IssuedCoupon`은 대상 categoryId만 박제, 멤버십은 `findSelfAndDescendantIds`로 주문 시 해소 |

### 0.2 추정 [추정] — 컨벤션 수준, 구현 시 확정

설계 판단(VO 추출·minOrderAmount 위치·정률 정밀도·긴급 중단)은 grill로 닫혀 원장 #24~#27로 옮겼다. 여기 남은 건 명칭·매핑 수준의 낮은 위험 추정뿐이다.

| 항목 | 추정안 | 위험도 | 비고 |
|---|---|---|---|
| 영속 매핑 | `DiscountRule` `@Embedded`, UNIQUE(policy_id, member_id) | 낮음 | JPA 연관 어노테이션 없음 유지 |
| API 표면 | `/api/v1/...` 경로, `CouponControllerV1` 등 | 낮음 | order(`/api/v1/orders`) 컨벤션 유도 |
| 팩토리·필드명 | `create`/`issue`/`reconstitute`, `issuableFrom/Until`, `usedOrderId`, Money 확장명 | 낮음 | order(`place`/`reconstitute`) 컨벤션 유도 |
| [v2] scope 매핑 | `ApplicabilityScope` `@Embedded`(type enum + targetId Long nullable), `coupon_policy`·`issued_coupon` 양쪽 | 낮음 | 연관 어노테이션 없음. WHOLE이면 targetId=null. BRAND/PRODUCT/CATEGORY는 각 대상 ID |
| [v2] 등급 override 매핑 | `gradeOverrides`를 `@ElementCollection` + `@MapKeyEnumerated`(MemberGrade) + `@Embedded DiscountRule`, `coupon_policy_grade_override` 테이블 | 낮음 | Aggregate 내부 value 컬렉션, `ddd.md §5` 예외 허용 범위 |
| [v2] MemberGrade 매핑 | `Member.grade` `@Enumerated(STRING)` 컬럼, 가입 기본값 `BRONZE` | 낮음 | member 도메인 변경. 기존 행 마이그레이션 기본 `BRONZE` |
| [v2] DiscountableLine | coupon 도메인 중립 record(`amount`, `productId`, `brandId`, `categoryId`) | 낮음 | 영속 대상 아님(transient 계산 입력). `categoryId`는 상품의 리프 카테고리 |

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

### `ApplicabilityScope` (공유 Value Object) [v2]

> [확정 #28·#29·#30·#31·#40] 할인이 어느 라인에 붙는지를 정의하는 VO. `CouponPolicy`가 보유하고 발급 시 `IssuedCoupon`으로 스냅샷.

| 필드 | 타입 | 비고 |
|---|---|---|
| type | `ScopeType` | `WHOLE` / `BRAND` / `PRODUCT` / `CATEGORY` (#40) |
| targetId | Long (nullable) | `WHOLE`이면 null, `BRAND`면 brandId, `PRODUCT`면 productId, `CATEGORY`면 categoryId |

- invariant: `WHOLE`이면 `targetId == null`, 그 외엔 `targetId != null`.
- 행위 — 매칭은 두 형태다.
  - **자족 매칭**(`WHOLE`/`BRAND`/`PRODUCT`): `matches(DiscountableLine line)` — `WHOLE`은 항상 true, `BRAND`는 `line.brandId == targetId`, `PRODUCT`는 `line.productId == targetId`.
  - **서브트리 매칭**(`CATEGORY`, #41·#42): 대상 카테고리의 서브트리(자기 + 모든 하위) id 집합이 필요하다. 이 집합은 순수 VO가 가질 수 없으므로(카테고리 트리는 다른 Aggregate) **application이 주문 시점에 `findSelfAndDescendantIds(targetId)`로 해소해 공급**한다. 매칭은 `line.categoryId ∈ 해소집합`. `matches(line, resolvedCategoryIds)` 형태로 받는다 `[추정: 시그니처]`. 상품은 리프 카테고리에만 매이므로 `line.categoryId`는 리프 id다.

### `CouponPolicy` (Root)

| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | |
| name | String | 캠페인명 |
| applicabilityScope | ApplicabilityScope | [v2 #28·#31] 할인 적용 범위 |
| discountRule | DiscountRule | [확정 #24] **기본 규칙**(등급 override 없을 때) |
| gradeOverrides | Map\<MemberGrade, DiscountRule\> | [v2 #35·#38] 등급별 할인 규칙 override. 부분 맵, 비어 있으면 비차등 |
| validDays | int | [확정 #13] 발급된 쿠폰의 유효일수 |
| issuableFrom / issuableUntil | ZonedDateTime | 발급 가능 기간(캠페인 창). `[추정: 필드명]` |
| maxIssueCount | long | [확정 #10·#11] 선착순 총량 |
| issuedCount | long | 발급 누적. 조건부 원자 UPDATE 대상(§10) |
| active | boolean | [확정 #27] 긴급 중단 스위치 |

- invariant: `validDays > 0`, `maxIssueCount > 0`, `issuableFrom < issuableUntil`. [v2] `applicabilityScope != null`, `gradeOverrides`의 각 값도 유효 `DiscountRule`.
- 팩토리: `create(...)`(id=null, issuedCount=0) / `reconstitute(...)`. `[추정: 명]`
- 행위:
  - `assertIssuable(now)`(발급 가능 기간 검증). `issuedCount` 증가의 **원자성은 repository 조건부 UPDATE가 보장**(§10). 도메인은 비즈니스 규칙만 책임진다.
  - [v2] `resolveRuleFor(MemberGrade grade): DiscountRule` — `gradeOverrides.getOrDefault(grade, discountRule)`. 발급 시 단일 규칙으로 해소(#36·#38). scope는 등급과 무관하게 고정이라 해소 대상이 아니다.

### `IssuedCoupon` (Root)

| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | |
| policyId | Long | 발급 출처, ID 참조 |
| memberId | Long | 소유자, ID 참조 |
| applicabilityScope | ApplicabilityScope | [v2 #28·#36] **스냅샷**(발급 시점 정책 scope 복사) |
| discountRule | DiscountRule | [확정 #5·#24][v2 #36] **스냅샷** = 발급 회원 등급으로 해소된 단일 규칙(`policy.resolveRuleFor(grade)`) |
| status | CouponStatus | [확정 #6·#8] `UNUSED` / `USED`(만료는 파생) |
| issuedAt | ZonedDateTime | 발급 시각. 도메인 사실로 직접 보유 |
| expiresAt | ZonedDateTime | [확정 #13·#14] **스냅샷**(= 발급일 + validDays, §9) |
| usedOrderId | Long (nullable) | [확정 #9] 복원 가드용(§6). `[추정: 명]` |

- 팩토리: [v2] `issue(policy, memberId, grade, now)` — `policy.resolveRuleFor(grade)`로 규칙 확정 후 scope·규칙·expiresAt 스냅샷 / `reconstitute(...)`. 등급은 발급 시점에만 읽혀 박제되므로(#36), 발급 후 등급이 바뀌어도 이 쿠폰의 혜택·범위는 불변.
- 행위 [확정 #20]:
  - [v2] `calculateDiscount(List<DiscountableLine> lines, Set<Long> resolvedCategoryIds)` — `applicabilityScope`로 매칭 라인 필터 → 부분합 산출 → `discountRule`에 위임(#24·#32). `CATEGORY` scope일 때만 `resolvedCategoryIds`(application이 `findSelfAndDescendantIds`로 해소, #42)를 멤버십 판정에 쓰고, 그 외 scope는 무시한다 `[추정: 시그니처]`. 매칭 라인 0이면 `BAD_REQUEST`("적용 대상 상품이 없습니다", #34). `WHOLE`이면 전 라인이 매칭돼 v1 동작과 동일.
  - [v2] `use(memberId, lines, resolvedCategoryIds, now, orderId)` — 소유자·상태·만료 검증 + `calculateDiscount(...)`로 매칭·최소금액 검증 후 `UNUSED→USED` + `usedOrderId` 기록. 동시 중복 사용 차단의 **원자성은 repository 조건부 UPDATE**(§10).
  - `restore(orderId)` — `USED→UNUSED`(§6). scope·등급과 무관하게 통째 복원이라 v1 그대로.
- `issuedAt`/`expiresAt`은 `BaseJpaEntity.createdAt`에 기대지 않고 쿠폰 도메인의 사실로 직접 보유한다.

### `DiscountableLine` (coupon 도메인 중립 입력) [v2]

> [확정 #32·#33] scoped 계산의 입력. coupon 도메인이 `OrderLine`(다른 Aggregate)을 모르게 하는 경계 차단막. 영속 대상 아님 — 주문 시점에 application이 즉석 조립 후 버린다.

| 필드 | 타입 | 비고 |
|---|---|---|
| amount | Money | 라인 금액(단가 × 수량) |
| productId | Long | `PRODUCT` scope 매칭용 |
| brandId | Long | `BRAND` scope 매칭용 |
| categoryId | Long | [#40] `CATEGORY` scope 매칭용. 상품의 리프 카테고리 id |

- `OrderPlaceUseCase`가 라인마다 이미 로드하는 `Product`(brandId·categoryId 보유)에서 조립한다(#33). `CATEGORY` scope면 추가로 `findSelfAndDescendantIds(targetId)`로 해소한 id 집합을 매칭에 공급한다(#42).

## 2. 스냅샷 (발급 = 그 순간 규칙의 박제)

[확정 #5] `IssuedCoupon`은 사용 시 `CouponPolicy`를 조인하지 않아도 할인을 계산할 만큼 규칙을 자족적으로 박제한다(OrderLine 스냅샷, order §2와 동일 논리). 발급 후 정책 수정이 기발급 쿠폰에 소급되지 않고(공정성), 주문 트랜잭션이 `CouponPolicy` Aggregate를 읽지 않는다. `expiresAt`도 발급 시 절대 시각으로 박제.

[v2 #36] 박제 대상이 셋으로 늘었다: **할인 규칙**(발급 회원 등급으로 해소된 단일 `DiscountRule`), **적용 범위**(`applicabilityScope`), **만료 시각**. 등급 차등은 발급 *시점에만* 등급을 읽어 규칙을 고르는 selection이고, 그 결과 하나만 박제되므로 v1의 "단일 규칙 박제" 불변식이 유지된다. 발급 후 회원 등급이 바뀌어도 이 쿠폰의 혜택은 고정된다. scope는 캠페인 정체성이라 등급과 무관하게 정책 값을 그대로 복사한다.

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
     → [v2] Member 로드 → grade            (발급 자격 회원의 등급)
     → [v2] IssuedCoupon.issue(policy, memberId, grade, now)
            policy.resolveRuleFor(grade)로 규칙 확정 + scope·expiresAt 스냅샷
     → IssuedCoupon INSERT                 (UNIQUE(policy_id, member_id) → 중복 시 CONFLICT)
     → 조건부 quota UPDATE                   (0행이면 소진 → BAD_REQUEST, Txn 롤백)
     → commit
```

- [v2 #36·#37] 등급은 **발급 시점에만** 읽힌다. `CouponIssueUseCase`가 `MemberRepository`로 회원을 로드해 `grade`를 얻고, 정책이 그 등급의 규칙(override ?? 기본)을 골라 박제. 등급은 주문 흐름엔 등장하지 않는다.
- [확정 #12] **1인 1매**: `(policy_id, member_id)` UNIQUE. 동시 중복 발급도 DB가 차단.
- [확정 #10·#11] **선착순 총량**: `UPDATE coupon_policy SET issued_count = issued_count + 1 WHERE id = :id AND issued_count < max_issue_count`. 0행이면 소진, 초과 발급을 원천 차단한다.

**1 Txn 정합성**: INSERT(유니크 가드)와 quota UPDATE(소진 가드)를 한 트랜잭션에 둔다. 어느 가드가 실패하든 예외 → 전체 롤백이므로, 순서와 무관하게 부분 효과(quota만 증가)가 남지 않는다. 유니크 위반은 도메인 예외(`CONFLICT`)로 변환해 롤백시킨다.

**의식적 규칙 예외: 발급 Txn의 다중 Aggregate 수정** — `ddd.md §4`("1 Txn = 1 Aggregate")와 충돌하나 의식적 예외다(order Txn1이 SKU+Order를 묶은 것과 동일 성격). 발급은 `CouponPolicy`(카운트) + `IssuedCoupon`(생성)을 한 Txn에 묶어야 초과 발급을 막고, 외부 호출이 없어 락 점유가 짧다.

## 5. 사용 흐름 (주문 통합)

[확정 #6] `OrderPlaceCommand`에 선택 입력 `couponId` 추가. 쿠폰 소비는 재고 차감과 동일 패턴으로 order §4의 2단계 흐름에 얹는다.

```
Txn1: (기존) member·sku·product·brand 검증 + 라인 스냅샷 + 재고 차감
      → couponId 있으면: IssuedCoupon 로드 + 소유자(memberId) 검증
      → [v2] 라인마다 이미 로드한 Product(brandId·categoryId)로 DiscountableLine(amount, productId, brandId, categoryId) 조립
      → [v2] CATEGORY scope면: categoryRepository.findSelfAndDescendantIds(targetId)로 서브트리 id 집합 해소 (#42)
      → discount = issuedCoupon.calculateDiscount(lines, resolvedCategoryIds)   ([v2] scope 매칭→부분합→할인, §7)
      → Order.place(memberId, lines, discount, couponId)    (payableAmount 계산, §8)
      → Order(PAYMENT_PENDING) 저장
      → couponId 있으면: 저장된 orderId로 조건부 쿠폰 사용 UPDATE (§10, 0행이면 이미사용/만료 → BAD_REQUEST)
      → commit
결제 호출   ← 트랜잭션 밖
   payableAmount == 0 이면 게이트웨이 스킵                    [확정 #21]
Txn2: 성공(또는 0원 스킵) → markPaid()
      실패 → cancel() + 재고 복원 + 쿠폰 복원(§6)
```

- 소유자 검증: `issuedCoupon.memberId == command.memberId` 불일치면 거부(단, 주문 `memberId`가 미인증이라 order의 인증 보류 수준에 종속, §15).
- [v2 #33] `DiscountableLine`은 박제하지 않는다. `OrderLine`엔 brandId를 저장하지 않고, 매칭은 주문 시점에 이미 로드된 `Product`에서 즉석 조립한 입력으로만 한다. 할인 결과(할인액·청구액)는 `Order`에 박혀 영구 보존되므로 재계산이 필요 없다.
- [v2 #34] scope에 매칭되는 라인이 없으면 `calculateDiscount`가 `BAD_REQUEST`("적용 대상 상품이 없습니다")로 거부 → Txn1 롤백. 조용히 0원 할인으로 통과시키지 않는다.
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

[v2 #29·#32·#34] 계산은 두 단계로 나뉜다. **scope 매칭·부분합**은 `IssuedCoupon`이(scope 스냅샷을 소유), **금액 계산**은 `DiscountRule`이(#24) 책임진다.

`IssuedCoupon.calculateDiscount(List<DiscountableLine> lines, Set<Long> resolvedCategoryIds)`:

```
A) matched = lines.filter(line -> scope에 매칭)             // WHOLE: 전부 / BRAND·PRODUCT: id 일치
                                                           // CATEGORY: line.categoryId ∈ resolvedCategoryIds (#41·#42)
B) matched 비어 있으면  → BAD_REQUEST("적용 대상 상품이 없습니다", #34)
C) base = Σ matched.amount                                  // 매칭 부분합 = 자격·할인 공통 기준
D) return discountRule.calculateDiscount(base)
```

- `CATEGORY`의 `resolvedCategoryIds`는 application이 주문 시점에 `findSelfAndDescendantIds(targetId)`로 해소해 넘긴다(#42). 다른 scope에선 이 인자를 보지 않는다. 상품이 리프에만 매이므로(`ProductRegisterUseCase`) 해소 집합과 `line.categoryId`(리프) 비교로 서브트리 포함이 판정된다.

`DiscountRule.calculateDiscount(Money base)` [확정 #16·#17·#18·#19]:

```
0) 자격: base < minOrderAmount  → BAD_REQUEST                          // [v2 #34] 기준 = 매칭 부분합
1) raw    = (type == FIXED) ? value : floor(base × value / 100)        // 정수 나눗셈 = 자연 floor
2) capped = (maxDiscountAmount != null) ? min(raw, cap) : raw
3) discount = min(capped, base)                                        // 부분합 초과분 클램프
```

- [v2 #34] 자격(`minOrderAmount`)과 할인 base가 동일하게 **매칭 부분합**이다(주문 전체 총액 아님). `WHOLE` scope면 부분합 = 주문 총액이라 v1과 동일.
- 클램프로 할인액 ≤ 매칭 부분합 ≤ 주문 총액 → 청구액 최소 0, `Money` 비음수 invariant 유지.

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
| 정책 생성(관리자) | `CouponPolicyCreateCommand`([v2] scope·gradeOverrides 포함) | `CouponPolicyInfo` | Txn. [v2] BRAND/PRODUCT scope면 대상 존재 검증(#31) |
| 쿠폰 발급(선착순) | `CouponIssueCommand`(policyId, memberId) | `CouponInfo` | §4 발급 Txn. [v2] Member 로드해 grade로 규칙 해소(#36) |
| 내 쿠폰 목록 | memberId, status?, page, size | `PageResult<CouponInfo>` | readOnly. 만료는 파생 필터 |
| (주문 생성 확장) | `OrderPlaceCommand`(+couponId) | `OrderInfo` | order UseCase에 쿠폰 소비 삽입(§5) |

명칭은 `[추정]`(order 컨벤션 유도). 신규 발급/조회는 두 Repository만, 주문 생성은 기존 `OrderPlaceUseCase`에 `IssuedCouponRepository` 의존을 추가한다. 결제 실패·주문 취소의 재고/쿠폰 복원은 application의 `OrderCompensationHelper`가 묶어 수행한다.

[v2] **정책 생성의 대상 존재 검증**(#31·#40): `BRAND`→`BrandRepository`, `PRODUCT`→`ProductRepository`, `CATEGORY`→`CategoryRepository.findById`로 대상 ID 존재를 확인하고 없으면 `NOT_FOUND`. 죽은 쿠폰(존재하지 않는 대상으로 영원히 매칭 0)을 발급 전에 차단한다. `CATEGORY`는 루트·중간·리프 어느 깊이든 대상이 될 수 있다(서브트리로 해소되므로, #41).

[v2 #42] **주문 생성의 서브트리 해소**: `OrderPlaceUseCase`에 `CategoryRepository` 의존을 추가한다. 쿠폰 scope가 `CATEGORY`일 때만 `findSelfAndDescendantIds(targetId)`를 호출해 해소 집합을 쿠폰 계산에 넘긴다. 다른 scope면 호출하지 않는다.

[v2] **발급의 등급 조회**(#36·#37): `CouponIssueUseCase`가 `MemberRepository.findById(memberId)`로 회원을 로드해 `grade`를 얻고 `IssuedCoupon.issue(policy, memberId, grade, now)`에 전달한다. 등급은 발급 시점에만 읽힌다 — 주문 생성 UseCase는 등급을 모른다.

## 12. 5계층 매핑 `[추정: 경로·명칭]`

- **interfaces** (`com.commerce.interfaces.api.coupon`): `CouponControllerV1`(`/api/v1/coupons` 발급, `/api/v1/members/{memberId}/coupons` 내 쿠폰), `CouponPolicyControllerV1`(`/api/v1/coupon-policies`). `ApiResponse<T>` 직반환(프로젝트 컨벤션). [v2] `CouponPolicyCreateRequest`에 scope·gradeOverrides 필드 추가.
- **application** (`com.commerce.application.coupon`): UseCase, `*Command`/`*Info`. [v2] `CouponPolicyCreateUseCase`에 `BrandRepository`·`ProductRepository`·`CategoryRepository` 의존 추가(대상 검증), `CouponIssueUseCase`에 `MemberRepository` 의존 추가(등급 조회). `OrderPlaceUseCase`(application.order)에 `CategoryRepository` 의존 추가(CATEGORY 서브트리 해소, #42).
- **domain** (`com.commerce.domain.coupon`): `CouponPolicy`/`IssuedCoupon`/(`DiscountRule`)/`DiscountType`/`CouponStatus`, `CouponPolicyRepository`·`IssuedCouponRepository`. [v2] `ApplicabilityScope`/`ScopeType`(scope VO), `DiscountableLine`(중립 입력 record) 추가.
- **infrastructure** (`com.commerce.infrastructure.coupon`): `*JpaEntity`, JpaRepository(조건부 UPDATE 쿼리 포함), `RepositoryImpl`. `DiscountRule`은 `@Embedded` 검토. UNIQUE(policy_id, member_id). [v2] `ApplicabilityScope` `@Embedded`(coupon_policy·issued_coupon 양쪽), `gradeOverrides`는 `@ElementCollection` 보조 테이블(`coupon_policy_grade_override`).
- **member 도메인 변경** [v2]: `com.commerce.domain.member`에 `MemberGrade` enum(`BRONZE`/`SILVER`/`GOLD`/`VIP`) 추가, `Member`에 `grade` 필드·`MemberJpaEntity`에 컬럼 추가. 가입 시 기본 `BRONZE`, 운영자 변경 메서드. 쿠폰 도메인은 `Member`를 ID로만 참조하고 발급 시 grade를 읽는다.

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
- **domain 단위** [v2]:
  - scope 매칭: `WHOLE`(전 라인) / `BRAND`(매칭 라인만 부분합) / `PRODUCT`(매칭 라인만) / `CATEGORY`(해소집합에 든 라인만, #41). 매칭 0 → "적용 대상 없음" 거부(#34).
  - CATEGORY 서브트리: 상위 카테고리 대상 + 해소집합(자기+하위)으로 하위 리프 상품이 매칭, 집합 밖 상품은 제외.
  - 최소금액 기준이 **매칭 부분합**임(주문 전체 아님): 전체는 충족하나 부분합은 미달 → 거부.
  - `ApplicabilityScope` invariant: `WHOLE`+targetId / 비WHOLE+null 거부.
  - 등급 해소: override 있는 등급은 override 규칙 박제, 없는 등급(예 BRONZE)은 기본 규칙 박제. 발급 후 등급 변경이 기발급 쿠폰에 무영향(#36).
- **동시성**(Testcontainers): 같은 정책 N 동시 발급 → `issuedCount ≤ maxIssueCount` + 1인 중복 차단. 같은 쿠폰 N 동시 사용 → 1회만 성공.
- **application**: 결제 성공/실패 목 → 쿠폰 `USED`/복원. 주문 취소 → 쿠폰 복원 + `payableAmount` 환불. 0원 주문 → 게이트웨이 미호출 + `PAID`.
- **application** [v2]: 등급 다른 두 회원이 같은 등급차등 정책 발급 → 각자 다른 규칙 박제. BRAND-scoped 쿠폰 주문 적용 end-to-end(매칭 부분합 할인). 존재하지 않는 brandId/categoryId로 정책 생성 → `NOT_FOUND`.
- **application** [v2 #42]: CATEGORY 쿠폰 발급 후 그 카테고리 서브트리에 하위 카테고리·상품을 추가 → 주문 시 신선 해소로 새 상품까지 매칭됨(발급 시점 박제였다면 누락). 서브트리 밖 카테고리 상품은 미매칭.
- `@WebMvcTest`: 발급·내쿠폰·정책생성 + 쿠폰 적용 주문. ArchUnit `LayerDependencyTest` · Spotless 그린.

## 15. 보류·확장 지점 (의식적으로 미룸)

미결(권장 기본값만 둔 것)은 §0.2로 이동했다. 여기는 **기능 확장**만 둔다.

- **orphan/멱등성** (order §8·§14 상속): 결제 성공 후 Txn2 커밋 전 크래시 → 쿠폰 `USED`·재고 차감·주문 `PAYMENT_PENDING`·돈 빠짐. `POST /orders` 재시도 시 둘째 시도가 조건부 UPDATE 0행으로 실패. 주문 멱등키 작업과 한 묶음.
- ~~상품/브랜드 한정 쿠폰~~ → **v2에서 구현**(`PRODUCT`/`BRAND` scope, #29~#34).
- ~~CATEGORY 한정 쿠폰~~ → **v2 후속에서 구현**(계층형 카테고리 도메인 도입 후 서브트리 매칭, #40~#42).
- **다중·교차 대상** [v2 보류 #30]: 단일차원 다중ID(`BRAND in {A,B}`), 다차원 교차(`카테고리Y 안의 브랜드X`). 규칙 엔진 필요.
- **등급 발급 게이트** [v2 보류 #35]: "등급≥X만 수령". 차등(#35)과 직교한 별도 축(eligibility). 필요 시 `issuanceEligibility` 추가.
- **등급별 scope 차등** [v2 보류]: 등급에 따라 적용 범위까지 바뀌는 쿠폰. scope는 캠페인 정체성이라 등급 무관 고정으로 둠.
- **등급 자동 산정** [v2 보류 #37]: 누적 구매액·이력 기반 승급/강등 배치. 현재는 운영자 수동 변경.
- 스택킹(주문당 N장, 적용 순서·배분).
- 부분취소 시 쿠폰 부분 복원(라인 단위 할인 배분 필요). v2 #33(라인 박제 안 함) 결정과 맞물려, 부분취소 도입 시 `OrderLine`에 brand/category 박제를 재검토.
- 관리자 일괄 발급·프로모 코드·이벤트 자동 발급.
- 고트래픽 선착순: Redis 원자 카운터(`modules/redis` 첫 사용처, Redis와 DB 정합성 보정비용).
- 인증·인가 강제 적용(현재 소유자 검증은 `memberId` 비교 의존).
