# 핸드오프 — product 도메인 (2026-06-13, 세션 마감)

> 현재 코드 기준 보정: 이 문서는 2026-06-13 당시 핸드오프 기록이다. 이후 Brand/Category/Cart/Order/Coupon/Auth 경계 작업과 Testcontainers 기반 통합 테스트가 추가되어, 아래 항목 중 일부는 현재 상태 메모로 갱신했다.

product 도메인을 DDD 5계층으로 구현. **domain·infra·application·interfaces + @WebMvcTest 완료.**
당시 빌드/테스트 그린(160개), Spotless 통과. 당시에는 실 DB 부팅·영속성이 미검증이었지만, 현재는 `IntegrationTestSupport` 기반 `@SpringBootTest` + Testcontainers MySQL 통합 테스트가 추가되어 일부 영속성·컨텍스트 부팅을 검증한다.

검증: `./gradlew :apps:commerce-api:test :apps:commerce-api:spotlessCheck`

## 이번 세션에서 한 일

1. **🔴 블로커 해소** — `infrastructure/product/`에 잘못 들어간 SKU 5파일 삭제(빈 이름 충돌 원인).
2. **2차 위반 해소** — `ProductSearchCriteria`가 ArchUnit `*Criteria=application` 규칙과 충돌(domain Repository가 참조) → **`ProductSearchCondition`으로 개명**(domain 유지). 근거를 스펙 §4에 기록.
3. **`ProductSearchUseCase.search` 시그니처 변경** — 도메인 객체 입력 → 느슨한 파라미터. 현재 코드는 `(String keyword, Long categoryId, Long brandId, int page, int size)`를 받는다. 이유: interfaces는 domain import 금지(`LayerDependencyTest`)라 컨트롤러가 도메인 `ProductSearchCondition`을 못 만든다. UseCase가 내부 조립. `getDetail(Long)` 선례와 동일 스타일.
4. **interfaces 슬라이스 전부 구현** + **@WebMvcTest** 2종.
5. **DB 운영 설정 정리** (아래 §DB).

## 작업 방식 변경 (중요)

- **`behavior.md` #1 "작성하지 말고 제안하라" 폐기** → **프로덕션 코드 직접 작성**. 의도·트레이드오프 설명·DDD 준수는 유지. #6도 "한 번에 하나씩 변경"으로 갱신.

## 구현된 API 표면 (interfaces)

| 메서드 | 경로 | 비고 |
|---|---|---|
| GET | `/api/v1/products` | 목록/검색 `?keyword&categoryId&brandId&page&size`, **page 0-base 그대로 노출** |
| GET | `/api/v1/products/{productId}` | 상세 |
| POST | `/api/v1/admin/products` | 등록 |
| POST | `/api/v1/admin/products/{productId}/suspend` | 상태 전이 — **액션 엔드포인트**(도메인 메서드 1:1) |
| POST | `/api/v1/admin/products/{productId}/resume` | 상태 전이 — **액션 엔드포인트**(도메인 메서드 1:1) |
| POST | `/api/v1/admin/products/{productId}/discontinue` | 상태 전이 — **액션 엔드포인트**(도메인 메서드 1:1) |
| PATCH | `/api/v1/admin/skus/{skuId}/discount` | 할인 — 별도 `SkuControllerV1` |
| PATCH | `/api/v1/admin/skus/{skuId}/price` | 정가 변경 — 별도 `SkuControllerV1` |
| PATCH | `/api/v1/admin/skus/{skuId}/stock` | 재고 보충 — 별도 `SkuControllerV1` |

- 제네릭 `PageResponse<T>`(`interfaces.api`) — `PageResponse.of(pageResult, mapper)`.
- 상태/SKU 변경은 `ApiResponse.success()`(data 없음).
- 현재 고객 조회 API는 공개이고, 등록·상태·SKU 변경 API는 `/api/v1/admin/**` 경로와 `ADMIN` 권한으로 보호된다.

## DB / 스키마 운영 방식 (이번 세션 정리)

- **DB**: MySQL 8.0 (`docker/infra-compose.yml`). 호스트 포트 **3307**→3306, DB `commerce`, 계정 `application/application`.
- **배선**: 표준 `spring.datasource.*`가 아니라 커스텀 `datasource.mysql-jpa.main` 수동 Hikari 빈(`modules/jpa/src/main/java/com/commerce/config/jpa/DataSourceConfig.java`).
- **ddl-auto (프로파일별)**:
  - `local` = **none(상위 상속)** — 부팅해도 스키마/데이터 보존. (이번에 `create` 제거 → 시드가 안 날아가게)
  - `test` = **create** (컨텍스트 기동마다 새 스키마)
  - base/dev/qa/prd = none
- **base jdbc-url에 `/${MYSQL_DATABASE}` 추가**(이전엔 DB명 없어 test/프로파일리스가 잠재 함정이었음). local jdbc-url은 `localhost:3307/commerce` 하드코딩이라 env 불필요.
- **로컬 스키마 최초 생성 흐름**(local이 none이라 빈 DB엔 테이블 없음):
  ```bash
  docker compose -f docker/infra-compose.yml up -d mysql
  ./gradlew :apps:commerce-api:bootRun --args='--spring.jpa.hibernate.ddl-auto=create'  # 1회만
  ./gradlew :apps:commerce-api:bootRun   # 이후 일반 부팅(none) → 데이터 보존
  ```
- **시드 100만건은 사용자가 직접 INSERT 예정.** 옵션값은 별도 `@ElementCollection` 테이블(INSERT가 SKU 본체와 옵션값 테이블에 걸침). MySQL8 첫 접속 시 `?allowPublicKeyRetrieval=true&useSSL=false` 필요할 수 있음. 성능 테스트면 검색 인덱스(`products.status`·`products.name`·`skus.product_id`) 수동 추가 고려.
- **미정리(향후)**: jpa.yml의 dev/qa/**prd**가 jdbc-url을 `localhost:3307/commerce`로 하드코딩 → 실환경 세팅 시 정리 필요.

## 당시 검증 안 된 것 / 현재 상태

1. **전체 Spring 컨텍스트 부팅**: 당시 미검증. 현재는 `IntegrationTestSupport`가 `@SpringBootTest`로 컨텍스트를 띄우고 Testcontainers MySQL을 연결한다. 별도 로컬 `bootRun` smoke는 문서상 완료 근거로 확인되지 않았다.
2. **영속성 계층**: 당시 미검증. 현재는 `ProductBrandSearchIntegrationTest`가 `ProductRepository.search`의 ACTIVE 브랜드 게이트, `brandId`, `categoryIds` 필터를 실 MySQL로 검증하고, `StockDeducterConcurrencyTest`가 `SkuJpaEntity`와 재고 차감 전략을 실 MySQL에서 검증한다. Product/Sku 전체 저장·복원 왕복을 더 직접 검증하는 테스트는 여전히 보강 여지다.
3. **권한 경계**: 당시 TODO. 현재 상품 등록·상태 변경과 SKU 가격·재고 변경은 `/api/v1/admin/**` 경로와 `ADMIN` 권한으로 보호한다.

## 남은 작업

1. Product/Sku 저장·복원 왕복을 직접 검증하는 통합 테스트 보강.
2. 로컬 `bootRun` + MySQL smoke를 릴리스 체크로 별도 기록.
3. **future (§9)**: 검색엔진, 상품 기본정보 수정·옵션 추가/삭제, 다중 이미지, 외부 상품코드.
4. **future (§10)**: commerce↔WMS 이벤트 계약 → WMS 앱 → 주문 사가. 현재 별도 WMS 앱은 없다.

## 당시 다음 도메인 후보

| 후보 | 근거 | 비용 |
|---|---|---|
| **주문(Order)** ⭐ | 스펙이 "주문 도메인 만들 때 이 결정(ID참조·트랜잭션 경계)을 전제"라 명시. SKU를 Aggregate Root로 둔 이유가 OrderLine의 ID 참조. 재고 차감 동시성은 현재 주문 도메인에서 낙관적·비관적·조건부 원자 차감 3전략으로 구현됐고, WMS 사가는 아직 보류다. | 높음(다중 Aggregate 오케스트레이션·상태머신·재고예약) |
| 장바구니(Cart) | 구매 흐름상 주문 앞단. SKU ID 참조. 현재 구현 완료. | 중 |
| Category/Brand | product §9 마감. 현재 구현 완료. | 낮음 |

## 작업 규칙 (이 레포 고유)

- 프로덕션 코드 직접 작성 OK. DDD 5계층(`ddd.md`): domain 순수·JPA 연관 어노테이션 금지·ID 참조·1트랜잭션 1Aggregate·interfaces는 application만 의존(domain import 금지).
- UseCase: 클래스 Javadoc + 메서드 의도 주석.
- 커밋: 한국어, `Co-Authored-By` 금지. 외부 레포 참조 금지.
- 테스트: `should_결과_when_조건`, 한글 `@DisplayName`, `@Nested`, AssertJ, BDDMockito. WebMvcTest는 `tools.jackson`(Jackson 3), 에러 응답 `$.meta.result/errorCode/message`.

## 참조 아티팩트

- 스펙: `wiki/product/product-domain-spec.md` (§4 Condition 개명 근거 반영됨)
- 학습 노트: `wiki/learning/jpa-element-collection.md`
- 패턴 레퍼런스: `domain/member/Member.java`, `interfaces/api/member/MemberControllerV1Test.java`(WebMvcTest), `interfaces/api/ApiControllerAdvice.java`(에러 매핑), `interfaces/api/ApiResponse.java`
