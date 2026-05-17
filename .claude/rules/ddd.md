# DDD 코딩 규칙

CLAUDE.md "DDD 핵심 원칙"의 상세 규칙. Java 코드 작업 시 반드시 참조.

## 1) 5계층 분리

`apps/commerce-api` 내부는 5계층 패키지로 나눈다.

| 계층 | 패키지 | 책임 |
|---|---|---|
| **interfaces** | `com.commerce.interfaces.api.{도메인}` | HTTP 경계: Controller, Dto, ControllerAdvice. 외부 표현. |
| **application** | `com.commerce.application.{도메인}` | 유스케이스 단위 오케스트레이션: Facade, Info. **트랜잭션 경계**. |
| **domain** | `com.commerce.domain.{도메인}` | 비즈니스 규칙 핵심: Model(엔티티/VO), 도메인 Service, Repository **인터페이스**. |
| **infrastructure** | `com.commerce.infrastructure.{도메인}` | 영속화 어댑터: JpaEntity, JpaRepository, RepositoryImpl. 도메인 인터페이스를 구현. |
| **support** | `com.commerce.support.{관심사}` | CoreException, ErrorType 등 횡단 보조. |

## 2) 의존성 방향 (한 방향, 어긋나면 금지)

```
interfaces → application → domain ← infrastructure
                                   ↑
                              support (어디서든 참조 가능)
```

- **domain은 어디도 import 하지 않는다** (infrastructure, application, interfaces 모두 X). 순수 자바만 허용 (JPA 어노테이션도 금지).
- **infrastructure는 domain의 Repository 인터페이스를 구현**하고, 별도 JpaEntity로 매핑한다. 외부엔 domain 타입만 노출.
- application은 domain만 알고 infrastructure 구현체에 직접 의존하지 않는다 (스프링 DI로 인터페이스 주입).
- interfaces는 application의 Facade만 호출한다. domain Service를 직접 호출하지 않는다.

## 3) 도메인 모델은 자기 invariant를 스스로 지킨다

- 도메인 모델은 **순수 자바 class**. JPA 어노테이션(`@Entity`, `@Id`, `@Column`, `@Table` ...) 금지.
- public setter 금지. 의미 있는 메서드명(`changePrice(...)`, `markAsPaid()`)으로 상태 변경.
- 생성 시점과 상태 변경 시점에 invariant 검증. 위반 시 `CoreException(ErrorType.X, "구체 메시지")`을 던진다.
- 신규 생성과 영속 복원을 분리: `Order.place(...)` (ID 없음) / `Order.rehydrate(...)` (ID 있음).

## 4) Aggregate Root 단위 트랜잭션

- 트랜잭션 경계는 application의 Facade 메서드 단위. `@Transactional`은 Facade에 둔다.
- 하나의 트랜잭션 안에서는 하나의 Aggregate만 수정한다 (여러 Aggregate 동시 수정 금지).

## 5) 엔티티 연관관계 — JPA 연관관계 어노테이션 금지, 간접 참조(ID) 사용

- **JPA 연관관계 어노테이션을 사용하지 않는다**: `@ManyToOne`, `@OneToMany`, `@OneToOne`, `@ManyToMany`, `@JoinColumn`, `@JoinTable` 모두 금지.
- 엔티티 간 관계는 **상대 엔티티의 ID(Long) 또는 ID Value Object**로만 보유한다.
  - 잘못된 예: `@ManyToOne private Product product;`
  - 올바른 예: `private Long productId;` 또는 `private ProductId productId;`
- **Why**:
  - Aggregate 경계가 명확해진다 (객체 그래프가 경계를 슬쩍 넘지 못함).
  - LazyInitializationException · N+1 쿼리 같은 ORM 함정을 원천 회피.
  - 트랜잭션을 작고 명시적으로 유지 가능.
  - 데이터 흐름이 코드에서 한눈에 드러난다 (필요한 시점에 application 계층이 Repository를 명시 호출해 조립).
- **연관 데이터가 필요할 때**: application Facade에서 각 Repository를 순차 호출해 `Info` 객체로 조립한다. domain·infrastructure 계층이 다른 Aggregate를 끌어오지 않는다.
- 컬렉션 형태 자식(value 성격의 자식 엔티티)이 정말 필요하면 같은 Aggregate 내부에 한해 `@ElementCollection` 또는 `@Embedded` 활용 검토. 그래도 연관 어노테이션은 쓰지 않는다.

## 6) Repository 패턴 (도메인/영속 분리)

- 도메인 패키지에 `XxxRepository` **인터페이스** 정의 (e.g., `com.commerce.domain.product.ProductRepository`).
- 도메인 모델(`Xxx`)과 JPA 엔티티(`XxxJpaEntity`)는 **별도 클래스**로 분리한다.
- 인프라 패키지에 세 짝:
  - `XxxJpaEntity` — `@Entity` 매핑 전용. 도메인 ↔ 엔티티 변환 메서드(`fromDomain()`, `toDomain()`) 보유.
  - `XxxJpaRepository extends JpaRepository<XxxJpaEntity, Long>` — Spring Data 인터페이스.
  - `XxxRepositoryImpl implements XxxRepository` — 도메인 인터페이스 구현, JpaRepository 위임 + 매핑.
- JpaRepository는 **인프라 내부에 숨긴다**. 다른 계층에서 import 금지.
- `@Entity`, `@Table`, `@Column` 등 JPA 어노테이션은 **infrastructure 패키지 안에서만** 등장한다.

## 7) DTO 경계

- Controller 입출력은 Dto (interfaces 계층)만 사용. 도메인 객체를 외부에 노출하지 않는다.
- application 계층 출력은 `Info` 객체 (도메인 객체와 분리된 표현). Controller가 Info → Dto로 매핑.
- 도메인 → Info, Info → Dto 두 단계 매핑. 도메인을 외부 포맷에 끌려다니지 않게 격리.

## 8) Value Object 적극 활용

- 의미 있는 묶음(Money, Email, ProductId 등)은 단순 primitive·String 대신 record 기반 VO로.
- 불변, equals/hashCode는 값 기준. 도메인 invariant를 생성자에서 검증.

## 9) 유비쿼터스 언어

- 클래스명·메서드명·필드명은 비즈니스 용어 그대로. 약어·기술 용어 금지.
- 도메인 전문가와의 대화에서 쓰는 명칭과 코드의 명칭이 일치해야 한다.
