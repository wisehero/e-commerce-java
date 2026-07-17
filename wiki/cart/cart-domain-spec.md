# 장바구니(Cart) 도메인 스펙 v1

작성: 2026-06-13 · 상태: 구현 완료

회원·상품·주문 다음으로 만드는 도메인. 본 문서는 grill 세션에서 결정한 설계 합의를 기록한다.
이 도메인은 [`order` 스펙](../order/order-domain-spec.md)이 최초 범위에서 미뤄둔 **"장바구니→주문"** 의 카트 쪽 절반을 닫는다.
주문 스펙이 §11에서 보류한 **"동일 SKU 중복 라인 — 병합할지 거부할지"** 도 여기서 닫는다(§5).

## 0. 범위

이번 이터레이션: **카트 담기·수량변경·라인제거·비우기 + 실시간 조회(상태 플래그 포함)**.

- 카트는 **DB에 영속**한다. (Redis-TTL 후보를 검토했으나 의식적으로 RDB를 택했다 — §1.)
- **체크아웃(카트→주문)은 서버 주도 API로 제공한다.** application 계층이 CartLine을 주문 입력으로 변환한다(§7).
- 보류(의식적): 게스트(비회원) 카트, 버려진 카트 정리 배치, added_price 스냅샷("가격 변동" 알림), 동시성 `@Version`(§13).

## 1. 영속화 & Aggregate 구조 — DB(JPA), Order 패턴 미러링

**`Cart`가 Aggregate Root**, **`CartLine`은 고유 식별자를 가진 자식 엔티티**(Aggregate 내부). order §1과 동일한 구조다.

### 왜 Redis가 아니라 DB인가

카트는 휘발성·고변경·버려짐이 잦아 Redis Hash + TTL이 교과서적 적합지였다(미사용 `modules:redis`의 자연스러운 첫 사용처). 그럼에도 **RDB를 택했다.**

- 트레이드오프 수용: DB는 자동 만료가 없어 **버려진 카트 행이 누적**된다 → 정리 배치가 별도로 필요(§13에 TODO).
- 대신 얻는 것: member/product/order와 **동일한 Aggregate·Repository·트랜잭션 패턴**을 그대로 재사용. 읽기-수정-저장 일관성.

### 영속화 — `@OneToMany` 없이 두 테이블에 걸치기

프로젝트 규칙상 JPA 엔티티 연관 어노테이션이 전면 금지(`ddd.md §5`)다. order가 택한 방식(고유 id 자식 엔티티 + 별도 테이블)을 그대로 따른다.

- Cart Aggregate가 **`carts` + `cart_lines` 두 테이블에 걸친다.**
- `CartLineJpaEntity`는 고유 `id` + `cart_id`(`Long`, **ID 참조** — 연관 어노테이션 없음)를 가진다.
- `CartRepositoryImpl`이 `carts` 행과 `cart_lines` 행을 **명시적으로** 저장·조회·조립한다(캐스케이드 자동 저장 없음). order §1과 동일.
- **도메인엔 `CartRepository` 하나만** 둔다. `CartLineRepository`를 외부에 노출하지 않는다 — 라인은 Cart Aggregate를 통해서만 접근.

### `Cart` (Root)

| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | |
| memberId | Long | 소유자, ID 참조. member당 1카트 |
| lines | `List<CartLine>` | Aggregate 내부 자식 엔티티 |
| (createdAt/updatedAt) | BaseJpaEntity | 영속 메타. `updatedAt`은 §13 정리 배치 기준값 |

- 팩토리: `Cart.create(memberId)`(id=null, 빈 카트) / `Cart.reconstitute(...)`.
- 메서드: `addItem(skuId, qty)`, `changeQuantity(skuId, qty)`, `removeItem(skuId)`, `clear()` — §5.
- **총액을 저장하지 않는다** — 카트는 실시간이라 총액은 조회 시 현재가로 파생(§6·§8). 주문이 `totalAmount`를 저장한 것과 의식적으로 대비.

### `CartLine` (자식 엔티티)

| 필드 | 타입 | invariant |
|---|---|---|
| id | Long | 고유 식별자 |
| skuId | Long | **라인 식별 키**(§5). ID 참조 |
| quantity | int | ≥ 1 |

- **스냅샷 없음** — 상품명·옵션·가격·재고·판매상태를 박제하지 않는다(§2).
- 팩토리: `CartLine.create(skuId, qty)`(id=null) / `reconstitute(...)`.

## 2. 라인 = 실시간 참조 (스냅샷 아님) — 주문과의 핵심 대비

주문(OrderLine)은 **과거 사실의 박제**(상품명·옵션·`salePrice`를 주문 시점으로 동결)다. 장바구니는 그 반대다.

- `cart_lines`는 `(id, cart_id, sku_id, quantity)`만 들고, **가격·상품명·옵션·재고·판매상태는 조회 시점에 Product/Sku에서 다시 읽는다.**
- 그래서 카트는 **현재 구매 의도의 실시간 뷰**다. 담은 뒤 가격이 내려가면(할인) 현재가가, 품절되면 품절이, 단종되면 단종이 조회에 자연히 반영된다.
- 결과: 카트 조회는 자족적이지 않다(다른 Aggregate를 끌어와 보강해야 함). 이 조립은 application UseCase가 책임진다(§6).

| | OrderLine | CartLine |
|---|---|---|
| 성격 | 과거 사실 박제 | 현재 의도 실시간 참조 |
| 저장 | 상품명·옵션·단가 스냅샷 | skuId + quantity만 |
| 가격 | 주문 시점 `salePrice` 동결 | 조회 시 현재 `salePrice` |

## 3. 소유자 & 생명주기

- **소유자 = `memberId`.** interfaces 계층은 인증 principal에서 회원 ID를 읽어 application command의 `memberId`에 주입한다. **게스트(비회원) 카트는 보류**(§13)라서 비인증 사용자의 카트 담기 요청은 인증 경계에서 거부되고, 로그인 시 게스트 카트를 회원 카트로 병합하는 흐름도 없다.
- 첫 `addItem` 시 해당 `memberId`가 존재하는 회원인지 **검증**한다(없으면 `NOT_FOUND`). order의 "유령 구매자 차단"(order §8)과 같은 정신 — 유령 카트를 만들지 않는다.
- **member당 카트 1개**, **첫 `addItem` 때 지연 생성**. 카트가 없는 member의 조회는 404가 아니라 **빈 카트 뷰**(라인 0개, 총액 0)를 반환한다.

## 4. 검증 시점 — 카트=조언적, 주문=권위적

담기/조회/결제 각 시점에 Product·Sku 상태를 어디까지 보는지가 카트 도메인의 책임 경계를 가른다.

| 시점 | 검증 | 막는가 |
|---|---|---|
| **담을 때**(add/changeQty) | SKU 존재 + 소속 Product `ON_SALE` + Brand `ACTIVE` 구매가능 + 재고 ≥ 수량 | **막는다**(통과해야 담김) |
| **조회 때** | 실시간 재평가 → 라인 `status` 플래그(§6) | **막지 않는다**(표시만) |
| **결제 때** | — | 카트는 결제 안 함. 최종 검증·재고차감은 **주문 도메인**(order §4 Txn1) |

- 담을 때 막는 이유: 유령/판매중지/비활성 브랜드 SKU가 애초에 카트에 안 들어와 **카트가 깨끗**하다.
- 담은 뒤 상태는 변할 수 있으므로(품절·할인·단종·브랜드 비활성화) 조회 때 다시 평가하되 **라인을 막지 않고 플래그만** 단다 — 사용자가 보고 결정.
- **카트의 검증은 어디까지나 조언적**이다. 진짜 권위(재고 차감·오버셀 방지)는 주문 도메인이 가진다 → 두 도메인의 책임이 깔끔히 나뉜다.

## 5. 변경(mutation) 계약

`Cart` Aggregate가 메서드로 자기 invariant를 지킨다. **라인 식별 키는 `skuId`** 하나다(조회·수량변경·제거 전부 skuId).

| 메서드 | 동작 | invariant |
|---|---|---|
| `addItem(skuId, qty)` | 같은 SKU가 이미 있으면 **수량 합산(병합)**, 없으면 새 라인 | `qty ≥ 1`. 합산 결과를 그때 재고와 검증, 초과면 거부 |
| `changeQuantity(skuId, qty)` | 해당 라인 수량을 **절대값으로** 변경 | `qty ≥ 1`. `0`은 `BAD_REQUEST`(제거는 `removeItem`) |
| `removeItem(skuId)` | 라인 제거 | 없는 skuId면 멱등 no-op |
| `clear()` | 전체 비움(주문 완료 후 등) | — |

- 카트당 **최대 고유 라인 수 상한 50개**를 둬 Aggregate 비대를 막는다. 초과 시 `BAD_REQUEST`.

### 동일 SKU 병합의 근거 (order §11이 보류한 결정)

같은 SKU를 또 담으면 **수량을 합산**한다(별도 라인·거부 아님).

- 카트의 보편적 실제 동작.
- SKU는 이미 **옵션 조합까지 확정된 단위**(색상·사이즈 포함)라, 같은 skuId면 같은 물건 → 라인을 쪼갤 이유가 없다.
- 라인 식별이 `skuId` 하나로 단순해진다.

## 6. 조회(read) 모델 — 실시간 보강

`CartViewUseCase`가 조립한다(readOnly).

1. `CartRepository`로 Cart 로드.
2. 라인들의 `skuId`로 `Sku`/`Product`/`Brand`를 **배치 조회**(`findByIds`)해 현재 정보를 모은다.
3. 라인별로 보강 + 상태 판정해 `CartLineInfo`로 조립, `CartInfo`로 묶는다.

- **N+1 회피 필수**: 라인별 단건 조회 금지, 반드시 배치(`findByIds`)로 조립한다.
- 조립·다중 Aggregate 읽기는 **application이 오케스트레이션**한다. domain·infra는 다른 Aggregate를 끌어오지 않는다(`ddd.md §5`, order §9와 동일).

### `CartLineInfo` (application 파생)

| 필드 | 출처 |
|---|---|
| skuId, quantity | Cart(저장값) |
| productName, optionSummary | Product/Sku 실시간 조회. 옵션 요약 형식은 `Sku.optionSummary()`가 제공 |
| salePrice | Sku 실시간 조회(현재가) |
| lineSubtotal | `salePrice × quantity` 파생 |
| status | `PURCHASABLE` / `OUT_OF_STOCK` / `UNAVAILABLE`(판매중지·단종·브랜드 비활성) — Product.status + Brand.status + `Sku.hasEnoughStock(quantity)`로 파생 |

- `CartInfo.cartTotal` = **`PURCHASABLE` 라인 소계 합**. 못 사는 라인은 표시만 하고 총액에서 제외(체크아웃 금액과 일치).
- 가격은 항상 **현재가**를 보여주므로 "가격 변동" 별도 플래그가 불필요하다(그건 added_price 스냅샷이 필요해 §2 원칙과 충돌 — §13 보류).
- **`status`는 application(`CartLineInfo`)이 파생**한다. Cart 도메인은 skuId+qty만 알고 순수하게 유지(다른 Aggregate 상태를 모름). 단, SKU 자체의 표현/판단인 옵션 요약과 재고 충분성은 `Sku.optionSummary()`·`Sku.hasEnoughStock(quantity)`를 사용한다.

## 7. 체크아웃(카트→주문) — application 오케스트레이션

cart와 order 도메인 모델은 직접 결합하지 않는다.
대신 서버 주도 체크아웃 API에서 application 계층이 오케스트레이션한다.

```
클라: POST /api/v1/carts/checkout
서버: 인증 memberId 기준 Cart 조회
서버: CartLine → OrderPlaceCommand.LineCommand 변환
서버: sourceCartId를 포함해 주문 생성
서버: 주문이 PAID가 되면 정리 작업 저장 후 주문 라인 수량만 Cart에서 차감
```

- 주문 도메인은 카트 객체를 모른다. 주문에는 추적용 `sourceCartId`만 저장한다.
- 카트가 없으면 `NOT_FOUND`, 비어 있으면 `BAD_REQUEST`로 거부한다.
- 결제 실패 또는 주문 생성 실패 시 카트는 유지한다.
- `PAID` 반영과 함께 DB 정리 작업을 저장하고 즉시 정리가 실패하면 스케줄러가 재시도한다.
- 정리 작업은 `orderId`를 멱등 키로 사용하며, 현재 카트에서 주문 수량만 차감한다.
- 정리 작업은 별도 비즈니스 Aggregate가 아니라 트랜잭션 진행 상태를 보존하는 기술적 process record다.
- 정리 전에 주문이 사용자 취소되면 카트는 건드리지 않고 작업만 완료한다.
- 전체 체크아웃만 지원한다. 부분 체크아웃은 §13 확장 지점으로 남긴다.

## 8. 금액 모델

- 라인 소계·카트 총액은 조회 조립 시점에 SKU의 현재 `salePrice.amount()`를 읽어 `long` 응답 금액으로 파생한다(`lineSubtotal = salePrice × quantity`, `cartTotal = 구매 가능 라인 소계 합`).
- **총액을 저장하지 않는다** — 카트는 실시간이라 조회 시 현재가로 매번 파생한다. 주문이 `total_amount`를 영속화한 것(과거 청구 사실)과 의식적으로 대비된다. `cart_lines`엔 가격 컬럼이 없다.

## 9. 유스케이스 (application, `@Transactional`)

| UseCase | 입력 | 출력 | 비고 |
|---|---|---|---|
| 카트 담기 | `CartAddItemCommand`(memberId, skuId, quantity) | `CartInfo` | member 존재검증 → 지연 생성 → 담기 검증(§4) → 병합(§5) |
| 카트 수량 변경 | `CartChangeQuantityCommand`(memberId, skuId, quantity) | `CartInfo` | 절대값 변경 + 재고 검증 |
| 카트 라인 제거 | (memberId, skuId) | `CartInfo` | |
| 카트 비우기 | memberId | — | `clear()` |
| 카트 체크아웃 | `CartCheckoutCommand`(memberId, lockMode, couponId?) | `OrderInfo` | 전체 카트를 주문으로 전환, `PAID` 시 정리 작업 실행 |
| 체크아웃 카트 정리 | orderId | — | 주문 수량 차감, 실패 작업 재시도 |
| 카트 조회 | memberId | `CartInfo` | readOnly, 실시간 보강(§6), 배치 조회 |

- 담기/수량변경 UseCase 직접 의존: `CartRepository` + `PurchasableItemResolver` + `CartInfoAssembler`(+ 담기 시 `MemberRepository`). SKU·상품·브랜드 조회와 구매 가능성 검증은 `PurchasableItemResolver`가 맡아 주문과 같은 기준을 공유한다.
- 조회 보강은 `CartInfoAssembler`가 `SkuRepository.findByIds`, `ProductRepository.findByIds`, `BrandRepository.findByIds` 계열 배치 조회로 수행한다.

## 10. 5계층 매핑

- **interfaces** (`com.commerce.interfaces.api.cart`): `CartControllerV1`(`/api/v1/carts`), `*Request`/`*Response`(`PageResponse` 불필요 — 카트는 단건).
  - `GET /api/v1/carts` → 인증 회원 카트 조회
  - `POST /api/v1/carts/items` (`skuId`, `quantity`) → 인증 회원 카트에 담기
  - `POST /api/v1/carts/checkout` (`lockMode`, `couponId?`) → 인증 회원 카트 체크아웃
  - `PATCH /api/v1/carts/items/{skuId}` (`quantity`) → 인증 회원 카트 수량변경
  - `DELETE /api/v1/carts/items/{skuId}` → 인증 회원 카트 라인 제거
  - `DELETE /api/v1/carts` → 인증 회원 카트 비움
  - `memberId`는 요청 body/query/path로 받지 않고 인증 principal에서 가져온다.
- **application** (`com.commerce.application.cart`): 위 UseCase들, `CartCheckoutCleanupUseCase`, `CartAddItemCommand`/`CartChangeQuantityCommand`, `CartInfo`/`CartLineInfo`(+`status` enum).
- **domain** (`com.commerce.domain.cart`): `Cart`/`CartLine`, `CartCleanupTask`, Repository 인터페이스. 순수 자바, JPA 어노테이션 없음.
- **infrastructure** (`com.commerce.infrastructure.cart`): Cart·CartLine·CartCleanupTask JPA 매핑과 Repository 구현. JPA 연관 어노테이션 없이 ID 참조.

## 11. 구현 시 주의

- **`ErrorType` 신규 없음**: `NOT_FOUND`(member/sku/product 없음, 변경·체크아웃 대상 카트 없음), `BAD_REQUEST`(재고 부족·`qty<1`·`changeQuantity(0)`·라인수 상한 초과·구매 불가 상태), `CONFLICT`(이전 정리 작업 미완료). product §7·order §12 가이드 따름.
- `CartRepository`: 일반 조회와 `PESSIMISTIC_WRITE` 조회, `save(Cart)`(두 테이블)를 제공한다. 카트 비우기는 `Cart.clear()` 후 `save`로 반영한다.
- 조회 보강은 `CartInfoAssembler`가 `SkuRepository.findByIds`, `ProductRepository.findByIds`, `BrandRepository.findByIds`를 사용한다. 담기·수량변경의 단건 구매 가능성 검증은 `PurchasableItemResolver`가 각 Repository의 `findById`로 수행한다.
- 카트 변경과 체크아웃 정리는 Cart 루트·라인을 `PESSIMISTIC_WRITE` 현재 읽기로 잠근다. 라인 전량 교체는 bulk DELETE 후 재삽입해 오래된 스냅샷 행 ID로 인한 충돌을 피한다.
- `Cart.create`(빈 카트)/`reconstitute` 분리. status 파생은 application에서(§6), 도메인에 넣지 않는다.

## 12. 검증 기준

- domain 단위테스트: `addItem` 병합(수량 합산), `changeQuantity` 절대값·`qty<1` 거부, `removeItem`, `clear`, 라인수 상한.
- application 테스트: 담기 검증(미존재 SKU·비-`ON_SALE`·재고부족 거부), 지연 생성, 조회 보강 + `status` 판정(`PURCHASABLE`/`OUT_OF_STOCK`/`UNAVAILABLE`), `cartTotal` 계산, 체크아웃 주문 수량 차감, 정리 작업 실패·재시도·멱등성.
- N+1 회피 검증: `CartInfoAssemblerTest`에서 라인 여러 개 조회 시 `SkuRepository`/`ProductRepository`/`BrandRepository`를 라인별 단건 호출하지 않고 `findByIds` 배치 호출로 조립함을 고정한다. 실제 SQL 쿼리 카운터 기반 통합 검증은 아직 없다.
- `@WebMvcTest`: 6개 엔드포인트, 에러 응답(`$.meta.result/errorCode/message`), 빈 카트 200 응답.
- ArchUnit `LayerDependencyTest`(cart도 동일 규칙) · Spotless 그린.

## 13. 보류·확장 지점 (의식적으로 미룸)

- **버려진 카트 정리 배치**: DB는 TTL이 없어 행이 누적된다. `@Scheduled`로 `updated_at` N일 경과 카트 삭제(DB의 TTL 대체) — **별도 배치 이터레이션**. TODO 명시.
- 게스트(비회원) 카트: 익명 식별자(쿠키/세션) 기반. 현재는 인증 회원 카트만 지원하며, 비회원 카트 저장·로그인 후 회원 카트 병합은 구현되어 있지 않다.
- added_price 스냅샷("담을 때보다 가격 변동" 알림): §2 실시간 원칙과 충돌하므로 보류.
- 부분 선택 체크아웃: 현재는 전체 카트만 체크아웃한다. 선택 정책이 필요해지면 주문된 라인만 제거하고 라인 출처 스냅샷을 검토한다.
- 동시성 `@Version`(§11), 위시리스트/저장 목록 분리.
