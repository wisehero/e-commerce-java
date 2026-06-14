# 상품(Product) 도메인 스펙 v1

작성: 2026-05-31 · 상태: 구현 완료(이번 이터레이션 범위)

회원 다음으로 만드는 첫 비-회원 Aggregate. 본 문서는 grill 세션에서 결정한 설계 합의를 기록한다.
주문 도메인을 만들 때 이 결정들(특히 ID 참조·트랜잭션 경계)을 전제로 한다.

## 1. Aggregate 구조

**별도 Aggregate 2개**로 분리하고, 서로 **ID(Long)로만 참조**한다.

- `Product` — 카탈로그 표현 단위.
- `Sku` — 실제 판매 단위(옵션 조합 + 가격 + 재고). 자체 Aggregate Root.

### 왜 SKU를 별도 Aggregate로 두는가

- 주문 라인(OrderLine)은 "팔리는 단위" = SKU를 ID로 참조한다. DDD에서 Aggregate 경계를 넘는 ID 참조는 **Root만** 가리킬 수 있으므로, SKU가 Product 내부 자식이면 주문이 Product 내부를 가리키는 규칙 위반이 된다.
- SKU는 자체 식별자를 갖고 재고 차감 경합의 중심 → value 자식이 아니라 엔티티 → Root.
- 트레이드오프: "상품은 옵션 최소 1개" 같은 일관성은 트랜잭션이 아니라 application UseCase가 조율한다.

### `Product` (Root)

| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | |
| name | String | not blank, 길이 제한 |
| description | String | 상세설명 |
| categoryId | Long | ID 참조, 존재검증 보류 |
| brandId | Long | ID 참조, **필수**. 등록 시 존재 검증(브랜드 도메인 도입). 노출은 브랜드 ACTIVE 게이트를 함께 받음 — [`../brand/brand-domain-spec.md`](../brand/brand-domain-spec.md) |
| imageUrl | String | 대표 이미지 1장 |
| status | ProductStatus | `ON_SALE` / `SUSPENDED` / `DISCONTINUED` |

- 팩토리: `register(...)`(id=null, status=ON_SALE 기본) / `reconstitute(...)`.
- 메서드: `suspend()` `resume()` `discontinue()`.
- **`DISCONTINUED`는 단말 상태**(되돌리기 불가). 논리 삭제 역할을 겸하여, 단종 시 레코드는 남아 과거 주문 참조가 유지되고 목록·노출에서만 빠진다.
- **품절은 상태가 아니다** → SKU 재고에서 파생(`stock == 0`). 상품 전체 품절 = 모든 SKU 재고 0(application이 계산).

### 상태(ProductStatus) 의미와 노출 규칙

상태(판매 의도)와 품절(재고)은 **별개 축**이다. `ON_SALE`인데 모든 SKU 재고가 0이면 화면엔 "품절"로 보이지만 상태는 여전히 `ON_SALE`이다.

| 상태 | 의미 | 노출(목록·검색·상세) | 구매 | 전이 |
|---|---|---|---|---|
| `ON_SALE` | 정상 판매중 | 노출(브랜드 ACTIVE일 때) | 가능(브랜드 ACTIVE + 해당 SKU 재고 > 0일 때) | → SUSPENDED, → DISCONTINUED |
| `SUSPENDED` | **일시** 판매중지(복귀 가능) | 제외 | 불가 | → ON_SALE(resume), → DISCONTINUED |
| `DISCONTINUED` | **영구** 단종(논리 삭제) | 제외 | 불가 | 없음(**단말**) |

- **노출 = `ON_SALE` + 브랜드 `ACTIVE`.** 구매자 대상 조회(목록·검색·상세) 어디서도 `SUSPENDED`·`DISCONTINUED`나 비활성 브랜드 상품은 보이지 않는다. 상세 조회 시 비-`ON_SALE` 또는 비활성 브랜드면 `NOT_FOUND`. → `Product.isVisible()` = `status == ON_SALE`, 최종 고객 게이트는 브랜드 상태를 함께 본다.
- `SUSPENDED` vs `DISCONTINUED`: 전자는 **복귀(resume) 가능**한 일시중지, 후자는 **되돌릴 수 없는** 영구 종료(삭제 대체).
- 전이 규칙(`Product`가 소유, 위반 시 `CoreException`):
  - `register()` → `ON_SALE`
  - `suspend()`: `ON_SALE` → `SUSPENDED`
  - `resume()`: `SUSPENDED` → `ON_SALE` (`DISCONTINUED`면 거부)
  - `discontinue()`: `ON_SALE`/`SUSPENDED` → `DISCONTINUED` (이미 `DISCONTINUED`면 거부)

### `Sku` (Root)

| 필드 | 타입 | invariant |
|---|---|---|
| id | Long | |
| productId | Long | ID 참조 |
| optionValues | `List<OptionValue>` | `@ElementCollection`, 최소 1개 |
| originalPrice | Money | ≥ 0 |
| salePrice | Money | **0 ≤ salePrice ≤ originalPrice** |
| stock | Stock | ≥ 0 — 판매 가능 재고 (실물 재고는 WMS 소유, §10) |

- 팩토리: `create(...)`(id=null) / `reconstitute(...)`.
- 메서드: `applyDiscount(Money)` `clearDiscount()` `changePrice(Money)` `restock(int)` `decreaseStock(int)`.
- **SKU 개별 상태 없음** — 옵션 판매 가능성 = `Product.status` + `Brand.status` + 해당 SKU 재고로 판단.

## 2. 가격 모델

- `salePrice`를 **항상 존재**시키고 기본값 = `originalPrice`.
  - `originalPrice`: 표시·취소선용 정가.
  - `salePrice`: **실제 결제·주문에 쓰는 가격**.
- "할인 중" = `salePrice < originalPrice`(파생), 할인율도 파생.
- 장바구니·주문은 항상 `salePrice` 하나만 읽으므로 가격 읽는 곳에 null 분기가 없다.
- 할인 자체의 주체(쿠폰/프로모션)는 별도 도메인. 여기 `salePrice`는 단순 직접 할인가일 뿐이다.

## 3. Value Object

- `Money(long amount)` — KRW 고정, 음수 금지. 원은 정수 통화라 `long`이면 정확하고 `BigDecimal` 복잡도를 피한다. `currency` 필드 없음(YAGNI). 연산 메서드는 주문이 합계 계산 시 필요해지면 추가.
- `Stock(int quantity)` — 0 미만 금지. `decrease(int)`/`increase(int)`가 새 `Stock` 반환(불변). commerce-api의 **판매 가능 재고**를 표현(실물 재고는 WMS 소유 — §10).
- `OptionValue(String name, String value)` — not blank. JPA 측은 `@Embeddable` + `@ElementCollection`.
- ID 참조는 **순수 Long**(ID VO 미사용).

## 4. 도메인 공통 추상(페이징·검색)

domain 순수성("어디도 import 안 함") 규칙을 지키기 위해 Spring Data `Pageable`/`Page`를 도메인에 노출하지 않는다.

- `PageResult<T>(List<T> items, long totalCount, int page, int size)` — **제네릭**, 위치: `com.commerce.support.page` (어디서든 참조 가능). `totalPages()`/`hasNext()` 파생 메서드 보유.
- `ProductSearchCondition(String keyword, Long categoryId, Long brandId, int page, int size)` — **상품 전용**, 위치: `com.commerce.domain.product`. 상품 전용 필드(keyword·categoryId·brandId)라 범용 support가 아닌 product 도메인에 둔다. domain `ProductRepository.search(...)`가 직접 참조하므로 domain에 있어야 한다. `keyword`·`categoryId`·`brandId`는 선택 필터(null 허용), `page`는 **0-base**.
  - **이름이 `*Criteria`가 아닌 이유**: `ddd.md §7`·ArchUnit 규칙상 `*Criteria`는 **application 계층 전용** 조회 경계 객체다. 이 객체는 domain Repository 인터페이스가 소유하는 query 객체(DDD Specification 성격)이므로 `*Criteria`로 명명하면 컨벤션과 충돌한다. 그래서 domain 적합 이름인 `Condition`을 쓴다. (초기엔 `ProductSearchCriteria`로 잘못 명명해 ArchUnit이 위반을 잡았고, 개명으로 해소.)
- infrastructure에서 `Condition → Spring Data Pageable`, `Page → PageResult` 변환. **Spring Data 의존은 infra에 가둔다.**

## 5. 유스케이스 (application, `@Transactional`)

| UseCase | 입력 | 출력 | 비고 |
|---|---|---|---|
| 상품 등록 | `ProductRegisterCommand` | `ProductDetailInfo` | Product+SKU **한 트랜잭션** |
| 상품 상세 조회 | productId | `ProductDetailInfo` | readOnly, Product+SKU 조립 |
| 상품 목록/검색 | `ProductSearchCriteria` | `PageResult<ProductSummaryInfo>` | readOnly, name LIKE + categoryId + brandId 필터 |
| 상품 상태 변경 | productId | — | suspend / resume / discontinue |
| SKU 가격 변경 | `SkuPriceChangeCommand` | — | applyDiscount / changePrice |
| SKU 재고 조정 | `SkuStockAdjustCommand` | — | restock |

- 목록·검색·상세에서 **`ON_SALE` + 브랜드 `ACTIVE`만 노출** (`SUSPENDED`·`DISCONTINUED`·비활성 브랜드 제외). 상세 조회 시 비-`ON_SALE` 또는 비활성 브랜드면 `NOT_FOUND`. 자세한 규칙은 §1 "상태 의미와 노출 규칙" 참조.
- 상세·조립 패턴: UseCase가 `ProductRepository` + `SkuRepository`를 각각 호출해 `Info`로 조립한다. 도메인·인프라가 다른 Aggregate를 끌어오지 않는다.

### 상품 등록을 한 트랜잭션으로 두는 근거 (규칙의 명시적 예외)

프로젝트 규칙 "1 트랜잭션 = 1 Aggregate **수정**"과 충돌하지만 의식적으로 예외를 둔다.

- 규칙은 "수정"을 말하며, 신규 **생성**은 기존 상태 변경이 아니고 경합이 없다.
- 상품과 그 옵션은 함께 태어나는 게 도메인상 자연스럽고, 쪼개면 "옵션 0개 상품"이라는 어색한 중간 상태 + 고아 상품 위험이 생긴다.
- 흐름: `POST /products`가 상품 + 옵션 목록을 한 번에 받아 한 `@Transactional`에서 Product 저장 후 SKU들 저장.
- 단, **수정**(가격/재고/상태)은 Aggregate별 독립 트랜잭션을 유지한다.

## 6. 5계층 매핑

- **interfaces**: `ProductControllerV1`(`/api/v1/products`), `*Request`/`*Response`.
- **application**: 위 UseCase들, `*Command`/`*Criteria`/`*Info`.
- **domain**: `Product`/`Sku`/VO/`ProductStatus`, `ProductRepository`·`SkuRepository`(인터페이스).
- **infrastructure**: `ProductJpaEntity`/`SkuJpaEntity`(+`OptionValue` `@Embeddable`), JpaRepository, RepositoryImpl(`Criteria→Pageable`, `Page→PageResult` 매핑). JPA 연관관계 어노테이션 없이 ID 참조.

## 7. 구현 시 주의

- **`ErrorType` 부족**: 현재 `INTERNAL_ERROR/BAD_REQUEST/NOT_FOUND/CONFLICT`만 존재. 새 타입 남발 대신 "재고 부족"은 `BAD_REQUEST` 재사용, "상품 없음"은 `NOT_FOUND` 사용을 권장.
- `SkuRepository`에 `saveAll`(등록용)·`findByProductId`(상세 조립용) 필요.
- 검색은 `Product.name` DB `LIKE`(이름 contains). 검색엔진 아님.
- 식별자는 `Long id`만(외부용 productCode/skuCode 없음). 대표 이미지는 URL 1장.

## 8. 권한

- 현재 인증 인프라 없음(`spring-security-crypto`로 BCrypt만 사용, Spring Security·SecurityConfig·로그인 없음).
- 상품 등록/수정/단종은 본질적으로 ADMIN 행위지만 **강제 적용 보류** — 의도는 TODO로 표시, 실제 권한 검증은 인증 도메인을 만들 때 일괄 적용.

## 9. 보류·확장 지점 (의식적으로 미룸)

- 재고 차감 동시성 락 → **주문 도메인**에서 결정(낙관적 `@Version` 유력).
- ~~Brand 도메인 + ID 존재 검증~~ → 해소: [`../brand/brand-domain-spec.md`](../brand/brand-domain-spec.md)(brandId 필수화·존재검증·노출 게이트). Category 도메인 + 존재 검증은 여전히 보류.
- 검색엔진(현재 DB LIKE).
- 인증/인가(현재 권한 보류).
- 상품 기본정보 수정·옵션 추가/삭제.
- 할인 = 쿠폰/프로모션 도메인(현재 `salePrice`는 단순 직접 할인가).
- 다중 이미지 갤러리(현재 대표 1장).
- 외부 상품코드/자연키(현재 Long id만).

## 10. 재고 컨텍스트 분리: commerce ↔ WMS (이벤트 연동)

재고는 **두 개의 다른 개념**으로 나뉘고 각각 별도 바운디드 컨텍스트가 소유한다. 별도 앱(`apps/commerce-api` ↔ `apps/wms`), 별도 DB, **Kafka 이벤트로만** 소통한다.

| 컨텍스트 | 재고 개념 | 권위를 갖는 전이 |
|---|---|---|
| commerce-api | 판매 가능 재고 (`Sku.stock`) | 주문에 의한 **차감** |
| WMS | 실물/창고 재고 | 입고·출고에 의한 **증감** |

- **핵심 원칙 — 전이별 단일 소유권.** 양방향 연동이지만 각 변경의 주인이 하나뿐이라 충돌·이중차감이 없다.
  - 입고(WMS 권위): WMS 입고 → 이벤트 → commerce 판매가능재고 증가.
  - 판매(commerce 권위): 주문 확정 → 이벤트 → WMS 실물 출고 차감.
- commerce-api는 WMS 재고를 수동 복제만 하는 게 **아니라** 자기 판매가능재고를 진짜 소유한다(주문 차감의 권위). read-only 복제 모델 아님.
- 두 수치는 같지 않아도 된다(판매가능 ≠ 실물). 단순화: 초기엔 WMS 입고분이 1:1로 판매가능재고로 통지된다고 가정(안전재고·부분 할당은 보류).

### 빌드 순서

1. commerce-api 카탈로그 도메인(`Sku.stock` 포함) — 본 문서.
2. commerce ↔ WMS **이벤트 계약**(토픽·스키마: 입고통지·판매차감 …) — **추후 상세**(별도 설계·문서).
3. `apps/wms` 최소 구현(`Inventory` Aggregate + 입고/조정) + 한 흐름 end-to-end 검증.
4. *(나중)* 주문 + 재고 예약/확정 **사가**(결과적 일관성·보상).

### 보류

- 이벤트 계약 상세(토픽명·스키마·순서/멱등성).
- 주문 시점 재고 예약·확정 사가 → order 도메인 단계.
- 안전재고·부분 할당·재고 실사 reconciliation.
