# 카테고리(Category) 도메인 스펙 v1

작성: 2026-06-18 · 상태: 구현 완료(이번 이터레이션 범위)

상품 정책의 보류 항목 "카테고리 검증 — 카테고리 도메인 도입 시 존재·리프 여부"와 brand 스펙의 보류 항목 "카테고리 도메인 + 존재검증"을 해소하기 위해 만든다.
본 문서는 grill 세션에서 결정한 설계 합의를 기록한다.
Category는 자기 자신을 부모로 참조하는 트리 Aggregate이며, Product와 ID 참조로 연결된다. 일부 product 결정(categoryId 검증, 검색 조건)을 의도적으로 변경한다(§8).

## 0. grill 결정 요약

| # | 결정 | 선택 |
|---|---|---|
| 구조 | 계층형 트리 (평면 아님) | |
| 깊이 | 고정 3단계(대>중>소). 노드에 `depth` 보유 | |
| 상품 부착 위치 | 리프(소분류, depth 3)에만 | |
| 상위 검색 | 하위 리프 상품까지 펼쳐서 포함(`categoryIds IN`) | |
| 이름 유일성 | 같은 부모 아래 형제끼리만. 루트는 부모 NULL 분기 | |
| 상태 모델 | `ACTIVE` / `INACTIVE` 2단계(노출 토글) | |
| 삭제 | 하드 삭제 + 자식 있으면 거부(상품 매달림 검증은 다음 단계) | |
| 이동(재부모) | 미허용 | |
| 부가 필드 | `sortOrder`(형제 정렬). 아이콘/이미지는 보류 | |
| 조회 | 전체 트리 한 방(중첩 children) + 단건 상세 | |
| product 연동 | 리프 검증 + 검색 펼침은 별도 단계로 분리 후 진행 | |

## 1. Aggregate 구조

**단일 Aggregate Root `Category`**. 자기 자신을 `parentId`(Long)로 가리키는 자기 참조 트리다. JPA 연관 어노테이션 없이 ID로만 참조한다.

### `Category` (Root)

| 필드 | 타입 | invariant |
|---|---|---|
| id | Long | |
| name | String | not blank, ≤ 50자 |
| parentId | Long | 루트는 null, 비루트는 not null |
| depth | int | 1~3 |
| sortOrder | int | 형제 정렬 순서 |
| status | CategoryStatus | `ACTIVE` / `INACTIVE` |

- 팩토리: `registerRoot(name, sortOrder)`(parentId=null, depth=1, ACTIVE) / `registerChild(name, parentId, parentDepth, sortOrder)`(depth=parentDepth+1) / `reconstitute(...)`.
- 메서드: `rename(String)` `changeSortOrder(int)` `activate()` `deactivate()` `isVisible()` `isLeaf()`.

### 트리 invariant (도메인이 스스로 지킴)

- 이름은 1~50자.
- `depth`는 1~3. `registerChild`에서 부모 depth가 3이면 자식 depth가 4가 되어 거부된다("최하위에는 하위를 추가할 수 없음").
- 루트(`parentId == null`)는 반드시 depth 1.
- 비루트(`parentId != null`)는 depth ≥ 2.
- `isLeaf()`는 depth == 3(고정 3단계의 최하위)일 때 참. 상품 부착 검증에 쓴다.

부모 존재 여부와 형제 이름 중복은 application UseCase가 검증한다(다른 인스턴스 조회가 필요하므로 도메인이 아닌 application 책임). 도메인은 자신이 받은 값만으로 invariant를 본다.

## 2. 이름 유일성 (형제 범위)

- 같은 부모 아래 형제끼리만 유일. DB는 `(parent_id, name)` 복합 unique(`uk_categories_parent_name`).
- **루트 문제**: 루트는 `parent_id`가 NULL이고 MySQL은 NULL 값들의 중복을 막지 않으므로, 복합 unique만으로는 루트 이름 중복을 못 막는다. application에서 `existsByParentIdAndName(null, name)`을 **`existsByParentIdIsNullAndName`(파생 쿼리)으로 분기**해 막는다.
- `rename`: 같은 이름이면 무동작 허용, 다른 이름이면 형제 중복 재검증.

## 3. 검색 펼침 (상위 → 하위)

상품은 리프에만 붙으므로, 상위 카테고리로 검색하면 그 하위 리프들의 상품을 모아야 한다.

```
검색 categoryId 주어짐 → 자신 + 모든 하위 카테고리 id로 펼침 → product를 categoryId IN (...)로 필터
```

- `CategoryRepository.findSelfAndDescendantIds(categoryId)`가 자신 + 후손 id를 모은다. 트리가 3단계 고정이므로 **직속 자식 + 손자까지 두 단계만 펼치면** 모든 하위가 모인다(직속 자식 1회, 손자 IN 1회). closure table 같은 장치는 쓰지 않는다.
- `ProductSearchUseCase`가 펼침을 수행하고, 결과 id 목록을 `ProductSearchCondition.categoryIds`로 넘긴다. `categoryId`가 없으면 `categoryIds`는 null(필터 미적용).
- product 검색 쿼리는 `(:categoryIds IS NULL OR p.categoryId IN (:categoryIds))`로 필터한다. 컬렉션 파라미터의 `IS NULL`(필터 없음)과 `IN`(목록 필터)이 실제 MySQL에서 동작함을 통합 테스트로 확인했다.

## 4. 유스케이스 (application, `@Transactional`)

| UseCase | 입력 | 출력 | 비고 |
|---|---|---|---|
| 카테고리 등록 | `CategoryRegisterCommand` | `CategoryInfo` | parentId 없으면 루트, 있으면 부모 조회 후 depth+1. 부모 없음 `BAD_REQUEST`, 형제 이름 중복 `CONFLICT`, depth 4 `BAD_REQUEST`(도메인) |
| 카테고리 수정 | `CategoryUpdateCommand` | `CategoryInfo` | rename + changeSortOrder, 이름 실제 변경 시에만 형제 중복 재검증 |
| 카테고리 활성화 | categoryId | — | `INACTIVE` → `ACTIVE` |
| 카테고리 비활성화 | categoryId | — | `ACTIVE` → `INACTIVE`, 자유(하위·상품 미조회) |
| 카테고리 삭제 | categoryId | — | 없으면 `NOT_FOUND`, 자식 있으면 `CONFLICT`, 아니면 하드 삭제 |
| 카테고리 단건 조회 | categoryId | `CategoryInfo` | readOnly |
| 카테고리 트리 조회 | — | `List<CategoryTreeInfo>` | readOnly, `findAll` 후 parentId 그룹핑으로 메모리 조립, sortOrder 정렬 |

- 트리 조립은 전체 조회 후 메모리에서 부모-자식으로 묶는다. 3단계 고정이라 전체를 내려도 부담이 작다.

## 5. 5계층 매핑

- **interfaces**: `CategoryControllerV1`(`/api/v1/categories`), `CategoryRegisterRequest`/`CategoryUpdateRequest`/`CategoryResponse`(단건)/`CategoryTreeResponse`(중첩 children).
- **application**: 위 UseCase들, `Category{Register,Update}Command`, `CategoryInfo`/`CategoryTreeInfo`.
- **domain**: `Category`/`CategoryStatus`, `CategoryRepository`(인터페이스).
- **infrastructure**: `CategoryJpaEntity`(`categories` 테이블, `uk_categories_parent_name`), `CategoryJpaRepository`, `CategoryRepositoryImpl`.

### API

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/v1/categories` | 전체 트리(중첩) |
| GET | `/api/v1/categories/{id}` | 단건 상세 |
| POST | `/api/v1/categories` | 등록 |
| PATCH | `/api/v1/categories/{id}` | 이름·정렬 수정 |
| POST | `/api/v1/categories/{id}/activate` | 활성화 |
| POST | `/api/v1/categories/{id}/deactivate` | 비활성화 |
| DELETE | `/api/v1/categories/{id}` | 삭제(자식 있으면 거부) |

## 6. ErrorType · 권한

- **`ErrorType` 재사용**(새 타입 남발 금지): 형제 이름 중복 `CONFLICT`, 부모 없음·리프 아님·깊이 초과 `BAD_REQUEST`, 단건/삭제 대상 없음 `NOT_FOUND`, 자식 있는 삭제 `CONFLICT`.
- 카테고리 등록·수정·삭제는 본질적으로 ADMIN 행위지만 **강제 적용 보류** — 인증 도메인을 만들 때 일괄 적용(brand·product 스펙과 동일 기조).

## 7. 인프라 처리 (자기 참조 트리)

- `CategoryRepositoryImpl.findSelfAndDescendantIds`는 `findIdsByParentId`(직속 자식 id)와 `findIdsByParentIdIn`(손자 id)을 조합한다. id projection 쿼리(`SELECT c.id ...`)로 엔티티 전체 로드를 피한다.
- 삭제는 `existsByParentId`로 자식 유무를 확인한 뒤 `jpa.deleteById`로 물리 삭제한다. `BaseJpaEntity`의 soft delete 마킹은 쓰지 않는다(노출 제어는 status가 담당).

## 8. product 도메인 변경 (categoryId 검증·검색 영향)

이 문서가 product의 기존 결정을 의도적으로 갱신한다.

코드 변경:
- `ProductRegisterUseCase`에 `CategoryRepository` 주입, 등록 시 카테고리 **존재 + 리프** 검증 추가. 없으면 `BAD_REQUEST`, 리프 아니면 `BAD_REQUEST`.
- `Category.isLeaf()` 추가(depth == 3).
- `ProductSearchCondition`의 `categoryId`(Long) → `categoryIds`(List&lt;Long&gt;)로 변경.
- product 검색 쿼리를 `p.categoryId = :categoryId` → `(:categoryIds IS NULL OR p.categoryId IN (:categoryIds))`로 변경.
- `ProductSearchUseCase`에 `CategoryRepository` 주입, `categoryId`를 `findSelfAndDescendantIds`로 펼쳐 `categoryIds`로 전달.
- 관련 테스트 갱신(리프 검증 케이스 추가, 검색 펼침 검증, 통합 테스트에 `categoryIds` IN/null 케이스).

문서 갱신:
- `../product/01-product-policy.md` §3·§8·§10(카테고리 리프·펼침·검증 해소).
- `../product/product-domain-spec.md` §1(categoryId 리프 검증)·검색 조건·보류 해소.

## 9. 빌드 순서 (실제 진행)

1. **Category 도메인 + infra + application + interfaces** — §1·§4·§5. 계층별로 컴파일·테스트 검증.
2. **product 연결** — 리프 검증(`ProductRegisterUseCase`) + 검색 펼침(`ProductSearchUseCase`·`ProductSearchCondition`·쿼리)(§8).
3. **문서화** — 본 문서 + product/brand 문서의 카테고리 보류 항목 해소(§8).

## 10. 보류·확장 지점 (의식적으로 미룸)

- 비활성 카테고리 상품 노출 게이트(검색 연동 시 brand 게이트와 같은 방식).
- 카테고리 삭제 시 상품 매달림 검증(product 연동 단계).
- 카테고리 이동(재부모) — 순환 방지·깊이 재계산 동반, 별도 유스케이스.
- 아이콘/이미지 필드.
- 인증/인가(카테고리 운영 행위 권한 강제 적용).
