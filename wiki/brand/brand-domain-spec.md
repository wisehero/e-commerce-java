# 브랜드(Brand) 도메인 스펙 v1

작성: 2026-06-13 · 상태: 구현 완료(이번 이터레이션 범위)

상품 정책의 보류 항목 "브랜드 검증 — 브랜드 도메인 도입 시 존재 여부와 노출 정책"을 해소하기 위해 만든다.
본 문서는 grill 세션에서 결정한 설계 합의를 기록한다.
Brand는 Product와 ID 참조로 연결되며, 일부 product 결정(brandId)을 의도적으로 변경한다(§8).

## 0. grill 결정 요약

| # | 결정 | 선택 |
|---|---|---|
| 역할 범위 | 자체 Aggregate + 노출 게이트 (셀러 개념 제외) | |
| 상태 모델 | `ACTIVE` / `INACTIVE` 2단계 (INACTIVE = 복귀 가능 + 논리 삭제 겸용) | |
| Product.brandId | 선택 → **필수**로 변경 | |
| 노출 게이트 | 목록·검색은 infra 조회 조인, 상세는 application 2단계 | |
| 이름 유일성 | 유일(정확 일치 의도, DB collation상 ci 동작) | |
| 고객 표면 | 상품검색 brandId 필터 + `GET /brands/{id}` 단건 + 상품응답 브랜드 포함 | |
| 등록 검증 | 존재만 검증(`existsById`), ACTIVE 불요 | |
| 비활성 가드 | 없음(자유 비활성화, 캐스케이드 없음) | |

## 1. Aggregate 구조

**단일 Aggregate Root `Brand`**. Product·Sku와 마찬가지로 별도 Aggregate이며 서로 **ID(Long)로만 참조**한다.

- `Product.brandId`가 `Brand`를 가리킨다(연관 어노테이션 없이 Long).
- Brand는 Product를 역참조하지 않는다. 브랜드는 자기가 어떤 상품을 가졌는지 모른다(경계 유지).

### `Brand` (Root)

| 필드 | 타입 | invariant |
|---|---|---|
| id | Long | |
| name | String | not blank, ≤ 50자, 유일 |
| logoUrl | String | nullable, 로고 이미지 1장 |
| status | BrandStatus | `ACTIVE` / `INACTIVE` |

- 팩토리: `register(...)`(id=null, status=`ACTIVE` 기본) / `reconstitute(...)`.
- 메서드: `rename(String)` `changeLogo(String)` `activate()` `deactivate()`.
- `description` 등 콘텐츠 필드는 보류(§10). 현재는 이름/로고/상태만.

### 셀러 개념을 두지 않는 이유

브랜드를 입점 판매자(seller/vendor)와 묶으면 정산·권한·입점 심사 같은 별개 관심사가 따라온다.
현재 범위는 카탈로그 분류와 노출 통제이므로, 셀러는 필요해질 때 별도 도메인으로 분리한다(YAGNI).

### 상태(BrandStatus) 의미와 노출 규칙

| 상태 | 의미 | 노출(목록·검색·상세·단건) | 전이 |
|---|---|---|---|
| `ACTIVE` | 정상 노출 | 노출 | → `INACTIVE`(deactivate) |
| `INACTIVE` | 노출 제외(논리 삭제 겸용) | 제외 | → `ACTIVE`(activate) |

- **2단계만 두는 이유**: product의 `SUSPENDED`(일시·복귀) / `DISCONTINUED`(영구·단말) 3단계는 "되돌릴 수 없음"을 보장해야 하는 업무적 실익(과거 주문 참조용 논리 삭제) 때문이었다. 브랜드는 재활성이 늘 가능해 그 구분이 불필요하다. `INACTIVE` 하나가 일시 숨김 + 철수(논리 삭제)를 겸한다.
- **물리 삭제 없음**: 비활성화가 곧 제거. 레코드는 남아 과거 주문·상품 참조가 유지된다.
- **자유 비활성화**: 상품 유무·상태와 무관하게 `deactivate()` 가능. `deactivate`는 product 측을 조회하지 않는다(aggregate 격리 유지, 캐스케이드 없음).
- 전이 규칙은 `Brand`가 소유(위반 시 `CoreException`).

## 2. 노출 게이트 (핵심 설계)

브랜드 상태가 상품 노출을 게이트한다.

```
고객 노출 = Product.status == ON_SALE  AND  Brand.status == ACTIVE
```

상품 상태와 브랜드 상태는 독립 축이고 둘 다 충족돼야 노출된다. 강제 적용 위치는 조회 형태에 따라 둘로 나눈다.

### 목록·검색 — infra 조회 조인

- product 목록/검색 쿼리가 **brand 테이블을 조인**해 `brand.status = ACTIVE`로 필터한다.
- 사후 메모리 필터는 페이징(totalCount·페이지당 개수)을 깨뜨리므로 쓰지 않는다. **단일 쿼리 조인**으로 정합성을 지킨다.
- JPA 연관 어노테이션 없이 `product.brand_id = brand.id` 수동 조인(JPQL/QueryDSL). Spring Data 의존은 infra에 가둔다.
- **트레이드오프(의식적 허용)**: product infra가 brand 테이블/상태 컬럼을 읽게 된다. 이는 **읽기(조회) 측** 한정이며, CQRS read-model이 테이블을 조인하는 것과 같은 성격이다. 쓰기/명령의 aggregate 격리("1 트랜잭션 = 1 Aggregate 수정", "다른 Aggregate를 객체로 끌어오지 않음")는 그대로 유지된다. 브랜드를 도메인 객체로 끌어오는 것이 아니라 상태 컬럼만 필터한다.

### 상세 — application 2단계

- product 상세 UseCase가 `ProductRepository`로 상품을, `BrandRepository`로 브랜드를 **각각 조회**해 조립한다(Sku 조립과 동일 패턴).
- 브랜드가 `INACTIVE`이면 `NOT_FOUND`(비-`ON_SALE` 상품과 동일 취급).
- 이 과정에서 읽은 브랜드의 이름/로고를 상세 응답에 그대로 쓴다(아래 §3 포함).

### 고객 브랜드 단건 조회

- `GET /brands/{id}`에서 브랜드가 `INACTIVE`이면 `NOT_FOUND`. 게이트 원칙과 일관.
- (운영/관리용 조회를 별도로 둘지는 권한 도메인과 함께 결정 — 현재 보류.)

## 3. 상품과의 연결 · 검증

- `Product.brandId` **필수**(§8에서 product 변경). 무브랜드 상품은 존재하지 않는다.
- 상품 등록 시 `brandRepository.existsById(brandId)`로 **존재만 검증**한다. ACTIVE는 요구하지 않는다(비활성 브랜드 출시 전 적재 허용). 존재하지 않으면 `BAD_REQUEST`.
- **브랜드 정보 포함**: 상세는 이미 2단계로 브랜드를 읽으므로 추가 비용 없이 브랜드 이름/로고를 `Info`에 담는다. 상품 응답이 `brandId`만이 아니라 `brandName`(필요 시 `brandLogoUrl`)을 함께 보여준다.

## 4. 이름 유일성

- `name` 유일. application에서 `trim` 정규화 후 `brandRepository.existsByName(name)` 체크 + DB unique 제약을 안전장치로 둔다. member 이메일/닉네임 중복 차단 패턴과 동일.
- **"정확 일치" 의도이나** MySQL 기본 collation(`utf8mb4_..._ci`)이 대소문자를 무시하므로 DB unique는 실제로 ci로 동작한다. 진짜 대소문자 구분(`Nike` ≠ `nike`)이 필요해지면 brand name 컬럼에 `_bin`/`_cs` collation을 지정한다. 현재는 불필요.
- `rename`: 같은 이름이면 무동작 허용, 다른 이름이면 중복 재검증.

## 5. 유스케이스 (application, `@Transactional`)

| UseCase | 입력 | 출력 | 비고 |
|---|---|---|---|
| 브랜드 등록 | `BrandRegisterCommand` | `BrandInfo` | `ACTIVE` 생성, 이름 중복 검증(`CONFLICT`) |
| 브랜드 수정 | `BrandUpdateCommand` | `BrandInfo` | rename / changeLogo, rename 시 중복 재검증 |
| 브랜드 활성화 | brandId | — | `INACTIVE` → `ACTIVE` |
| 브랜드 비활성화 | brandId | — | `ACTIVE` → `INACTIVE`, 자유(상품 미조회) |
| 브랜드 단건 조회 | brandId | `BrandInfo` | 고객 readOnly, `INACTIVE`면 `NOT_FOUND` |

- 등록·수정은 이름 중복 검증을 트랜잭션 안에서 수행한다.
- 상태 전이는 `Brand`가 소유하고 UseCase는 호출만 한다.

## 6. 5계층 매핑

- **interfaces**: `BrandControllerV1`(`/api/v1/brands`), `*Request`/`*Response`.
- **application**: 위 UseCase들, `*Command`/`*Info`.
- **domain**: `Brand`/`BrandStatus`, `BrandRepository`(인터페이스).
- **infrastructure**: `BrandJpaEntity`, `BrandJpaRepository`, `BrandRepositoryImpl`. JPA 연관 어노테이션 없이 ID 참조. **product 목록/검색의 brand 조인은 product infra 쿼리에서** brand 테이블을 참조해 처리(브랜드 도메인 타입을 끌어오지 않음).

## 7. ErrorType · 권한

- **`ErrorType` 재사용**(새 타입 남발 금지, product-spec §7 기조): 이름 중복 `CONFLICT`, 상품 등록 시 브랜드 없음 `BAD_REQUEST`, 고객 조회/상세 게이트 `NOT_FOUND`.
- 브랜드 등록·수정·상태변경은 본질적으로 ADMIN 행위지만 **강제 적용 보류** — 의도는 TODO로 표시하고, 실제 권한 검증은 인증 도메인을 만들 때 일괄 적용(product-spec §8과 동일).

## 8. product 도메인 변경 (brandId 필수화 영향)

brandId를 선택값 → 필수로 바꾼다. 이 문서가 product의 기존 결정을 의도적으로 갱신한다.

코드 변경:
- `Product.validate()`에 `brandId != null` 검증 추가.
- `ProductRegisterCommand` / `ProductRegisterRequest`의 brandId 필수화.
- `ProductRegisterUseCase`에 브랜드 존재 검증 추가(`BrandRepository.existsById`).
- 상품 목록/검색 쿼리에 brand `ACTIVE` 조인 게이트 추가.
- `ProductSearchCondition`에 optional `brandId` 필터 추가.
- 상품 상세 조립에 `BrandRepository` 조회 추가, `ProductDetailInfo`/`Response`에 브랜드 이름·로고 포함.
- 기존 product 테스트의 brandId null 케이스를 유효 brandId로 갱신.

문서 갱신:
- `01-product-policy.md` §3(브랜드 ID 필수로), §10(브랜드 검증 보류 → 해소).
- `product-domain-spec.md` §1 Product 표(brandId 필수·존재검증), §9(Brand 항목 → 해소).

## 9. 빌드 순서

1. **Brand 도메인 + infra + API** — 본 문서의 §1·§5·§6.
2. **product 연결** — brandId 필수화 + 등록 존재검증 + 조회 게이트 조인 + 검색 brandId 필터 + 상품응답 브랜드 포함(§8).
3. **product 문서 갱신**(§8).

## 10. 보류·확장 지점 (의식적으로 미룸)

- 브랜드 디렉토리(`GET /brands` 전체 목록·페이징, "브랜드관").
- `description` 등 브랜드 콘텐츠 필드.
- 셀러/입점사(정산·권한 단위) — 별도 도메인.
- 인증/인가(브랜드 운영 행위 권한 강제 적용).
- 출시 전(prelaunch) 상태 명시 — 현재는 존재검증만으로 비활성 브랜드 적재를 허용.
- 판매중 상품 보유 브랜드 비활성 가드 — 현재 자유 비활성화.
- ~~카테고리 도메인 + 존재검증~~ → 해소: [`../category/category-domain-spec.md`](../category/category-domain-spec.md)(3단계 트리, 리프 부착, 상위 검색 펼침).
