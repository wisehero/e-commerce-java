# 주문(Order) 도메인 스펙 v1

작성: 2026-06-13 · 상태: 합의 완료(설계), 구현 전

회원·상품 다음으로 만드는 첫 **다중 Aggregate 오케스트레이션** 도메인. 본 문서는 grill 세션에서 결정한 설계 합의를 기록한다.
이 도메인은 [`product` 스펙](../product/product-domain-spec.md)이 명시적으로 미뤄둔 두 결정을 닫는다 — **SKU를 ID로 참조하는 OrderLine**(product §1)과 **재고 차감 동시성**(product §9).

## 0. 범위

이번 이터레이션: **주문 생성(구매) + 동기 재고 차감 + 취소(재고 복원 + 환불)**.

- 결제는 **외부 API 스텁**(`sleep ~200ms`)으로만 흉내 낸다. 실제 PG·결제 도메인 없음.
- 보류(의식적): 배송·정산 라이프사이클, WMS 재고 사가(product §10), 장바구니→주문, 멱등성, 환불 실패 처리, 인증 enforce.

## 1. Aggregate 구조

**`Order`가 Aggregate Root**, **`OrderLine`은 고유 식별자를 가진 자식 엔티티**(Aggregate 내부).

### 영속화 — `@OneToMany` 없이 두 테이블에 걸치기

프로젝트 규칙상 JPA 연관 어노테이션이 전면 금지(`ddd.md §5`)다. 자식 컬렉션을 매핑하는 합법적 방법은 두 가지였다.

| 선택지 | 결정 |
|---|---|
| `@ElementCollection` 값 자식 (Sku.optionValues 방식) | ✗ — 라인에 고유 id가 없다 |
| **고유 id를 가진 자식 엔티티 + 별도 테이블** | ✓ **채택** |

- Order Aggregate가 **`orders` + `order_lines` 두 테이블에 걸친다.** 하나의 Aggregate가 여러 테이블에 매핑되는 건 정상이며, 그 Aggregate의 Repository가 전체 영속화를 책임진다.
- `OrderLineJpaEntity`는 고유 `id` + `order_id`(`Long`, **ID 참조** — 연관 어노테이션 없음. `Sku`가 `productId`를 들고 있는 것과 동일)를 가진다.
- `OrderRepositoryImpl`이 `orders` 행과 `order_lines` 행을 **명시적으로** 저장·조회한다. `@ElementCollection`과 달리 cascade 자동 저장이 없으므로, 저장(orders 저장 → 생성된 orderId로 라인 saveAll)과 조립(order 행 + `findByOrderId` → `Order.reconstitute`)을 RepositoryImpl이 직접 오케스트레이션한다.
- **도메인엔 `OrderRepository` 하나만** 둔다. `OrderLineRepository`를 외부에 노출하지 않는다 — 라인은 Order Aggregate를 통해서만 접근. (infra 내부에서 `OrderLineJpaRepository`를 주입받아 위임하는 건 구현 디테일.)

### 왜 자식 엔티티(고유 id)인가 / 트레이드오프

- 고유 id가 있으면 **미래의 부분 취소·부분 반품·라인 단위 참조**가 가능하다. `@ElementCollection`(값 자식)은 라인 식별이 안 돼 전체 단위로만 다룰 수 있다.
- 비용: 매핑 코드가 조금 더 든다(별도 엔티티 + 명시적 저장/조립, 자동 cascade 없음).
- 현재 범위는 "주문 전체 취소"라 부분 취소는 아직 안 쓰지만, 식별자를 미리 확보해 둔다.

### `Order` (Root)

| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | |
| memberId | Long | 구매자, ID 참조 |
| status | OrderStatus | `PAYMENT_PENDING` / `PAID` / `CANCELLED` |
| orderLines | `List<OrderLine>` | Aggregate 내부 자식 엔티티 |
| totalAmount | Money | **저장**. `place()`가 라인 합으로 스스로 계산 |
| orderedAt | ZonedDateTime | 주문 발생 시각. 도메인 사실로 직접 보유 |

- 팩토리: `Order.place(memberId, lines, orderedAt)`(id=null, status=PAYMENT_PENDING, totalAmount=라인 합) / `Order.reconstitute(...)`.
- 메서드: `markPaid()`, `cancel()`.
- `orderedAt`은 `BaseJpaEntity.createdAt`에 의존하지 않는다. `createdAt`은 영속 계층의 감사 컬럼이고, 주문 시각은 "언제 주문했는가"라는 도메인 사실이다. 두 값은 보통 같겠지만, 도메인 모델이 infrastructure 타입·정책에 기대지 않도록 별도 필드로 둔다.

### `OrderLine` (자식 엔티티)

| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | 고유 식별자 |
| productId | Long | 스냅샷 |
| skuId | Long | 스냅샷, 팔린 단위 |
| productName | String | **스냅샷**(주문 시점 상품명) |
| optionSummary | String | **스냅샷** 요약 문자열, 예: `"색상:블랙 / 사이즈:270"` |
| unitPrice | Money | **스냅샷** = 주문 시점 `Sku.salePrice` |
| quantity | int | ≥ 1 |

- `lineAmount() = unitPrice × quantity`(파생).
- 팩토리: `OrderLine.create(...)`(id=null) / `reconstitute(...)`.

## 2. 스냅샷 원칙 (주문 = 과거 사실의 기록)

OrderLine은 **조회 시 product/sku를 조인하지 않아도 주문 내역을 그릴 수 있을 만큼 자족적으로 박제**한다.

- 고객은 "그 순간의, 그 상품을, 그 가격에" 샀다. 나중에 상품명이 바뀌거나 SKU 옵션이 수정되거나 상품이 `DISCONTINUED`돼도 **주문 내역은 구매 당시 모습**을 그대로 보여줘야 한다.
- 스냅샷하면 주문 조회가 **자족적**이다 → 조회 시 다른 Aggregate를 끌어오지 않는다(`ddd.md §5`와 정합).
- `unitPrice`는 product §2대로 **`salePrice`**(실제 결제가). `originalPrice`(취소선용)는 주문 기록엔 불필요하다고 보고 제외.
- 옵션은 **요약 문자열 하나**로 박제(표시 전용). 구조가 필요해지면 `List<OptionValue>`로 승격(현재 YAGNI).

## 3. 상태머신 (3상태)

```
place()      → PAYMENT_PENDING        (Txn1: 재고 차감 + 주문 생성)
markPaid()   : PAYMENT_PENDING → PAID (Txn2: 결제 성공)
cancel()     : PAYMENT_PENDING → CANCELLED  (Txn2: 결제 실패 — 시스템 보상)
             : PAID            → CANCELLED  (유저 취소)
```

| 상태 | 의미 | 전이 |
|---|---|---|
| `PAYMENT_PENDING` | 주문 생성됨, 재고 차감 완료, 결제 결과 대기 | → PAID, → CANCELLED |
| `PAID` | 결제 성공(이번 범위의 성공 종착) | → CANCELLED(유저 취소) |
| `CANCELLED` | 주문 무효(결제 실패 보상 또는 유저 취소). **단말** | 없음 |

- 결제 실패(시스템 보상)와 유저 취소를 **같은 `CANCELLED` 종착 상태**로 모은다 — 결과(주문 무효 + 재고 복원)가 동일하기 때문. 사유 구분이 필요해지면 `cancelledReason` 추가(현재 YAGNI).
- 실패 시 **주문 레코드를 지우지 않고 `CANCELLED`로 남긴다** — `Product.DISCONTINUED`가 soft delete를 겸한 것과 같은 정신(과거 사실 보존·감사 추적).
- `PAYMENT_PENDING`은 주문 요청 안에서 ~200ms 내에 해소되는 transient 상태다. 유저가 별도 API로 잡을 수 없으므로, **유저 취소는 사실상 항상 `PAID` 주문이 대상**이다.

## 4. 주문 생성 흐름 — 2단계 동기 미니 사가

결제(외부 호출)와 재고 차감 트랜잭션의 관계가 핵심 설계점이다. **외부 호출을 트랜잭션 밖에 둔다.**

```
Txn1: memberId 존재검증
      → 각 라인 Product 조회(존재 · ON_SALE 구매가능)
      → 재고 차감(전략 주입, §6)
      → 차감 확정된 SKU 상태 기준으로 가격·옵션 스냅샷 + Product 이름 스냅샷
      → Order(PAYMENT_PENDING) 저장
      → commit (SKU 락 즉시 해제)
결제 호출(PaymentGateway.pay, stub ~200ms)   ← 트랜잭션 밖
Txn2: 성공 → order.markPaid()  /  실패 → order.cancel() + 라인별 Sku.restock(qty)
```

### 외부 호출을 트랜잭션 밖에 두는 이유

- 외부 호출을 `@Transactional` 안에 두면 **200ms 동안 차감한 SKU 행의 락을 쥔 채 대기**한다. 같은 인기 SKU를 사려는 다른 주문이 그만큼 줄서서 막힌다 → product §9가 풀라고 한 동시성 문제를 악화. (performance-profiler 스킬도 "트랜잭션 안 외부 호출"을 안티패턴으로 짚는다.)
- 밖으로 빼면 락 보유 시간이 짧은 DB 작업 시간으로 한정된다. 대신 중간 상태(`PAYMENT_PENDING`)와 실패 보상(취소 + 재고 복원)이 생긴다.

### 트랜잭션 경계 구현 원칙

`OrderPlaceUseCase`의 전체 주문 생성 오케스트레이션 메서드에는 `@Transactional`을 붙이지 않는다. 전체 메서드가 트랜잭션이면 결제 호출이 트랜잭션 안으로 들어가 §4의 핵심 원칙과 충돌한다.

- Txn1과 Txn2만 application 계층의 명시적 트랜잭션 경계로 둔다.
- 구현 선택지는 두 가지다.
  - `OrderPlaceUseCase`는 비트랜잭션 오케스트레이터로 두고, 같은 application 패키지의 별도 public 서비스/유스케이스 메서드에 `@Transactional`을 둔다.
  - 또는 `TransactionTemplate`으로 Txn1/Txn2를 코드에서 명시한다.
- 어느 쪽이든 외부 호출(`PaymentGateway.pay`)은 두 트랜잭션 사이에 있어야 한다.

### 의식적 규칙 예외 — Txn1의 다중 Aggregate 수정

`ddd.md §4` "1 트랜잭션 = 1 Aggregate 수정"과 충돌하지만 의식적으로 예외를 둔다. (product 등록이 둔 예외와 같은 성격.)

- Txn1은 **여러 `Sku` Aggregate 차감 + `Order` 생성**을 한 트랜잭션에 묶는다.
- 근거: 오버셀·고아 주문을 막으려면 재고 차감과 주문 생성이 **원자적**이어야 한다. 무거운 외부 결제 호출은 이 트랜잭션 **밖**이므로 락 점유는 짧게 유지된다.
- 재고 차감 자체의 동시성 안전은 §6의 락 전략이 책임진다.

### 스냅샷 일관성

주문 라인의 `unitPrice`·`optionSummary`는 **재고 차감이 성공한 SKU 상태**를 기준으로 박제한다. 가격 변경과 주문 생성이 동시에 들어왔을 때 "차감한 재고의 버전"과 "결제한 가격"이 어긋나지 않게 하기 위함이다.

- 낙관적/비관적 전략은 로드한 `Sku`에 `decreaseStock`을 적용한 뒤 그 도메인 객체의 `salePrice`·`optionValues`로 스냅샷을 만든다.
- 조건부 원자 UPDATE 전략은 도메인 모델을 우회하므로, UPDATE 성공 후 같은 트랜잭션 안에서 해당 SKU를 다시 읽어 스냅샷을 만든다. 이 전략은 성능 비교용 성격이 강하며, 도메인 invariant를 우회한다는 비용을 테스트와 문서에 남긴다.
- Product 이름과 구매가능 여부는 `Product.status == ON_SALE` 검증 시점에 읽은 값을 사용한다. 주문 이후 상품명이 바뀌어도 OrderLine 스냅샷은 변하지 않는다.

## 5. 취소 흐름

```
Txn: order.cancel()(PAID→CANCELLED) + 각 라인 skuId로 Sku.restock(qty) → commit
환불(PaymentGateway.refund, stub)   ← 트랜잭션 밖 (결제 호출과 동일 원칙)
```

- `PAID` 주문 취소는 **재고 복원 + 환불**을 동반한다. 결제를 흐름에 넣은 이상 환불도 대칭으로 있어야 일관적이다.
- 재고 복원은 `Sku.restock(count)`(product 도메인에 이미 존재) 재사용 — 차감의 역연산.
- **환불 실패 처리는 이번 이터레이션에서 보류**한다. 단, 이 보류는 "프로덕션 스텁이 항상 성공한다"는 전제에서만 안전하다.
  - 실제 PG 연결 전에는 취소와 환불을 같은 단순 상태로 묶으면 안 된다. `REFUND_PENDING`/`REFUNDED`/`REFUND_FAILED` 같은 환불 상태, 재시도, 수동개입 정책이 필요하다.
  - 지금 정책대로 실제 PG에 연결하면 "주문은 취소되고 재고는 복원됐지만 환불은 실패"한 불일치 상태가 생길 수 있다.

## 6. 재고 차감 동시성 — 3전략 전부 구현, 런타임 선택

product §9가 Order로 미뤄둔 결정. **세 전략을 모두 구현해 비교**한다(학습·벤치마크 목적).

### 추상화

- domain 포트 **`StockDeducter`** — `deduct(Long skuId, int quantity)`. 위치: `domain.product`(SKU 재고를 차감하는 능력이므로 SKU와 같은 패키지). application UseCase는 이 포트만 안다.
- infrastructure에 3구현(Strategy):

| 전략 | 구현 | 메커니즘 | 비교 포인트 |
|---|---|---|---|
| **낙관적 락** | `OptimisticStockDeducter` | `SkuJpaEntity`에 `@Version`. 로드→`Sku.decreaseStock`→save. 충돌 시 `OptimisticLockException` → **재시도 없이 fail-fast** → `CONFLICT` | 락 미점유, 핫 SKU 경합 시 실패 빈발 |
| **비관적 락** | `PessimisticStockDeducter` | `findByIdForUpdate`(PESSIMISTIC_WRITE) → `decreaseStock` → save | 직렬화, 재시도 불필요, Txn1 짧아 점유 부담 작음 |
| **조건부 원자 UPDATE** | `AtomicUpdateStockDeducter` | `UPDATE skus SET stock=stock-:q WHERE id=:id AND stock>=:q`, 영향행수 체크 | 가장 빠름, **도메인 모델(`Sku.decreaseStock`) 우회** |

### 전환 방법

- **운영용 주문 API는 고객에게 `lockMode`를 노출하지 않는다.** 동시성 전략은 내부 구현 정책이지 구매자가 고를 입력값이 아니다.
- 학습·벤치마크 목적의 비교가 필요하면 profile/config 또는 별도 test/admin endpoint에서만 전략을 전환한다.
- UseCase가 `Map<String, StockDeducter>`(Spring이 빈 이름으로 주입)에서 라우팅할 수는 있지만, public `POST /orders` 요청 모델에는 포함하지 않는다.

### 전제 결정 — 낙관적 락의 재시도

세 전략을 **모두 Txn1 안에서 동일 조건으로** 비교하기 위해, 낙관적 버전은 **재시도 없이 fail-fast**(충돌 → Txn1 롤백 → `CONFLICT`, 호출자가 재시도)로 둔다. 재시도를 넣으면 "attempt마다 새 트랜잭션" 구조라 트랜잭션 경계가 복잡해지고 비교 조건이 어긋난다. (재시도는 나중 개선 포인트.)

### `@Version` 위치

`SkuJpaEntity`에만 추가한다. `BaseJpaEntity`엔 넣지 않는다 — 거기 주석이 "재사용성을 위해 이 외 컬럼·동작 추가 금지"라 명시했고, 모든 Aggregate가 버전 락을 필요로 하지도 않는다.

## 7. 금액 모델

- `Money`(공유 VO)에 연산 추가: `plus(Money)`, `multiply(int quantity)`, 합산 시작점 `ZERO`. (product §3이 "주문 합계 계산 시 추가"로 미뤄둔 것.)
- 라인 금액 = `unitPrice.multiply(quantity)`, 총액 = 라인 합.
- **총액 저장**: `Order.place()`가 라인들로부터 합을 **스스로 계산**해 보유 → 결제 게이트웨이에 전달 → `orders.total_amount`로 영속화.
  - 근거: 총액 = 결제에 청구한 금액 = 구체적 사실. 라인 가격을 스냅샷하는 것과 같은 논리. 조회 시 재계산 불필요.
  - 무결성: 외부에서 총액을 받지 않고 도메인이 라인 합으로 계산 → 불일치 불가능.

## 8. 식별자 · 구매자

- **`Long id`만.** product·member 컨벤션(외부 코드 없음)과 일관. 주문번호·멱등키 보류.
  - **리스크 — 멱등성**: `POST /orders` 재시도 시 이중 주문·이중 결제 가능. 정석은 멱등키. 이번엔 보류하고 **TODO로 이중결제 위험 명시**.
- **구매자**: 요청 바디 `memberId` + `MemberRepository` 존재검증(없으면 `NOT_FOUND`). 실제 인증은 **TODO**(security context 유도, product admin 행위와 동일).
  - product가 `categoryId`/`brandId` 존재검증을 보류한 선례와는 의식적으로 다르게 간다 — 주문은 실제 결제를 일으키므로 유령 구매자를 차단.

## 9. 유스케이스 (application)

| UseCase | 입력 | 출력 | 비고 |
|---|---|---|---|
| 주문 생성 | `OrderPlaceCommand`(memberId, lines) | `OrderInfo` | 2단계 흐름(§4). Txn1/외부결제/Txn2 |
| 주문 취소 | orderId | — | 재고 복원 + 환불(§5) |
| 주문 단건 조회 | orderId | `OrderInfo` | readOnly |
| 회원별 주문 목록 | memberId, page, size | `PageResult<OrderInfo>` | readOnly |

- `OrderPlaceUseCase` 의존: `MemberRepository`·`SkuRepository`·`ProductRepository`(읽기 검증·스냅샷) + `OrderRepository`(쓰기) + `StockDeducter`(포트) + `PaymentGateway`(포트).
- 라인 스냅샷에 상품명이 필요하므로 `ProductRepository`로 Product를(상품명·`ON_SALE` 구매가능 여부), `SkuRepository`로 SKU를(가격·옵션·존재) 읽어 조립한다.
- `@Transactional`은 유스케이스 전체에 일괄 적용하지 않는다. 주문 생성은 §4의 Txn1/Txn2 경계에만 적용한다. 조회 유스케이스는 `readOnly` 트랜잭션을 둘 수 있다.

## 10. 결제 게이트웨이 (포트 + 스텁)

- domain 포트 **`PaymentGateway`** — `pay(...)`, `refund(...)`. 위치: `domain.order`(향후 결제 도메인이 생기면 이동 가능). 구현: `infrastructure.order`.
- 프로덕션 스텁: `sleep(~200ms)` 후 **항상 성공**.
- 실패→보상 경로는 **실패하는 `PaymentGateway` 목**으로 테스트에서 검증(포트 추상화 덕분에 프로덕션 코드에 테스트용 분기 없음).

## 11. 5계층 매핑

- **interfaces** (`com.commerce.interfaces.api.order`): `OrderControllerV1`(`/api/v1/orders`), `OrderPlaceRequest`/`OrderResponse`.
- **application** (`com.commerce.application.order`): 위 UseCase들, `OrderPlaceCommand`/`OrderInfo`(+`OrderLineInfo`).
- **domain** (`com.commerce.domain.order`): `Order`/`OrderLine`/`OrderStatus`, `OrderRepository`(인터페이스), `PaymentGateway`(포트). `StockDeducter` 포트는 `domain.product`.
- **infrastructure** (`com.commerce.infrastructure.order`): `OrderJpaEntity`/`OrderLineJpaEntity`, JpaRepository들, `OrderRepositoryImpl`(두 테이블 영속화·조립), `PaymentGateway` 스텁. 재고 차감 3구현은 `infrastructure.product`. JPA 연관 어노테이션 없이 ID 참조.

## 12. 구현 시 주의

- **`ErrorType` 신규 없음**: `NOT_FOUND`(member/sku/product/order 없음), `BAD_REQUEST`(재고 부족·invariant 위반·구매 불가 상태), `CONFLICT`(낙관락 충돌·오버셀 감지). product §7 가이드 따름.
- `OrderRepository`: `save`·`findById`·`findByMemberId(...)→PageResult<Order>`.
- `SkuRepository`에 `findByIdForUpdate`(비관 락용)·재고 차감 원자 UPDATE 메서드 추가 필요.
- 조회는 **본인 주문만** 보여야 정상(인증 붙으면 security context). 현재는 `memberId` 파라미터로 거른다(TODO).
- 페이징은 product가 만든 `PageResult`(`support.page`)·`PageResponse`(`interfaces.api`) 재사용.

## 13. 검증 기준

- domain 단위테스트: 상태 전이, 총액 계산, 취소 invariant, OrderLine 스냅샷.
- 동시성 테스트: 같은 SKU에 N 동시 주문 → 오버셀 없음(3전략 각각).
- application 테스트: 결제 성공/실패 목 → `PAID` / `CANCELLED`+재고복원.
- `@WebMvcTest`: 4개 엔드포인트, 에러 응답(`$.meta.result/errorCode/message`).
- ArchUnit `LayerDependencyTest` · Spotless 그린.

## 14. 보류·확장 지점 (의식적으로 미룸)

- 멱등성(이중 POST → 이중 결제 방지).
- 환불 실패 처리(재시도·수동개입).
- 인증/인가 enforce(현재 memberId 바디 전달 + TODO).
- 부분 취소·부분 반품(라인 id는 확보해 둠).
- 낙관적 락 재시도 전략.
- 배송·정산 라이프사이클, 주문번호(외부 노출).
- WMS 재고 예약·확정 사가(product §10) — 결과적 일관성·보상.
