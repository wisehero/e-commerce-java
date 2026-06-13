# JPA `@ElementCollection` — 값 컬렉션 매핑

엔티티가 **값(VO)의 컬렉션**을 가질 때 쓰는 매핑. 이 프로젝트에선 `Sku`의 옵션값
(`List<OptionValue>`)을 `sku_option_values` 테이블로 내릴 때 사용한다.
DDD 규칙이 `@OneToMany`는 금지하면서 `@ElementCollection`은 허용하는 이유와 직결된다.

## 1. 풀려는 문제

SKU 하나는 옵션값을 여러 개 가진다: `{색상=빨강}`, `{사이즈=L}`. 즉 `List<OptionValue>`.
관계형 DB의 한 컬럼에는 리스트를 담을 수 없으므로, 이 값들을 담을 **별도 테이블**이 필요하고
각 행은 "어느 SKU 소속인지"를 FK로 가리켜야 한다.

## 2. 어노테이션 3종의 역할

```java
@ElementCollection                                    // ① 값들의 컬렉션 (엔티티 아님)
@CollectionTable(                                     // ② 값들을 담을 별도 테이블
    name = "sku_option_values",                       //    - 테이블 이름
    joinColumns = @JoinColumn(name = "sku_id"))       //    - 소유자(SKU) id FK 컬럼
private List<OptionValueEmbeddable> optionValues;
```

| 어노테이션 | 역할 |
|---|---|
| `@ElementCollection` | "이 컬렉션은 독립 엔티티가 아니라 **이 엔티티에 종속된 값들**이다." 별도 `@Id`도, 별도 Repository도 없다. |
| `@CollectionTable` | 그 값들을 저장할 **별도 테이블**의 이름과 FK 컬럼을 정의. |
| `@JoinColumn` | 그 테이블의 각 행이 어느 소유자(SKU) 소속인지 가리키는 **FK 컬럼명**. |

원소 타입은 `@Embeddable`(또는 `String`·`Long` 같은 기본 타입)이어야 한다.
`@Embeddable` 안의 `@Column`들이 컬렉션 테이블의 컬럼이 된다.

```java
@Embeddable
public class OptionValueEmbeddable {
    @Column(name = "option_name", nullable = false, length = 50)
    private String name;
    @Column(name = "option_value", nullable = false, length = 100)
    private String value;
    // ...
}
```

## 3. 실제로 만들어지는 테이블

`skus`(SKU 본체)와 별개로 아래 테이블이 생성된다.

```
sku_option_values
┌────────┬─────────────┬──────────────┐
│ sku_id │ option_name │ option_value │
├────────┼─────────────┼──────────────┤
│   1    │   색상       │    빨강       │   ← SKU 1의 옵션 2개
│   1    │   사이즈     │    L         │
│   2    │   색상       │    파랑       │   ← SKU 2의 옵션 1개
└────────┴─────────────┴──────────────┘
```

SKU를 저장하면 옵션 행이 함께 INSERT되고, SKU를 삭제하면 함께 삭제된다.
**생명주기가 소유 엔티티(SKU)에 완전히 묶여 있다.**

## 4. `@ElementCollection` vs `@OneToMany`

| | `@OneToMany` | `@ElementCollection` |
|---|---|---|
| 대상 | **엔티티** (자체 `@Id`·생명주기·Repository 보유) | **값** (식별자 없음, 소유자에 종속) |
| 독립 존재 | 가능 (다른 Aggregate일 수 있음) | 불가 (소유자 없이 의미 없음) |
| 이 프로젝트 규칙 | **금지** (연관관계 어노테이션) | **허용** (Aggregate 내부 value 자식) |

`OptionValue`는 VO다 — 식별자가 없고 SKU 없이는 존재 의미가 없다. 그래서 Aggregate 경계
안에 갇힌 값 컬렉션을 매핑하는 정석 도구인 `@ElementCollection`이 정확히 맞는다.
반대로 `@OneToMany`는 다른 엔티티(잠재적으로 다른 Aggregate)로의 객체 그래프를 열어
경계를 흐리기 때문에 이 프로젝트에서 금지된다.

### `@JoinColumn` 금지 규칙과의 관계

DDD 규칙은 `@JoinColumn`을 금지 목록에 명시하지만, 그 취지는 **엔티티 연관 매핑**
(`@ManyToOne`/`@OneToMany`의 FK)을 겨냥한 것이다. `@CollectionTable` 안의 `@JoinColumn`은
value 컬렉션 테이블의 소유자 FK 컬럼명을 지정하는 용도라 엔티티 연관이 아니므로,
`@ElementCollection`을 허용하는 규칙의 취지에 부합한다고 본다.

## 5. 알아둘 동작 — 변경 시 전체 삭제 후 재삽입

값 컬렉션은 내용이 바뀌면 Hibernate가 보통 **해당 소유자의 컬렉션 행을 전부 DELETE한 뒤
다시 INSERT**한다(개별 행을 추적할 식별자가 없기 때문). 그래서:

- 변경이 잦은 큰 컬렉션에는 부적합 (성능). 그럴 땐 별도 엔티티 + Repository를 검토.
- 이 프로젝트의 `SkuJpaEntity.updateFromDomain`은 **옵션을 건드리지 않는다** — 현 스코프에서
  옵션은 불변이고, 가격·재고만 변경되므로 불필요한 delete+insert를 피한다.
- 컬렉션 필드를 통째로 재할당(`this.optionValues = newList`)하면 관리 중인 컬렉션과의
  연결이 끊겨 문제가 될 수 있다. 바꿔야 한다면 `clear()` + `add()`로 **제자리 변경**한다.

## 6. 기타

- 기본 fetch 전략은 **LAZY**. 따라서 `toDomain()`처럼 컬렉션을 읽는 코드는 트랜잭션(영속성
  컨텍스트가 열린 상태) 안에서 호출되어야 한다. 이 프로젝트에선 Repository 호출이 항상
  UseCase의 `@Transactional` 안에서 일어나므로 안전하다.
- 컬렉션 테이블에는 기본적으로 대리 PK가 없다. value 컬렉션에는 보통 문제되지 않는다.
