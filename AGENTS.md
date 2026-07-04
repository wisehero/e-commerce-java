# AGENTS.md

## 최우선

**세션 시작 시 `.agents/rules/behavior.md`를 반드시 읽는다.** 파일 수정 권한, 제안 형식, 대화 방식 등 모든 작업 방식이 거기에 정의되어 있다.

## 프로젝트 개요

Java 25 + Spring Boot 4 기반 이커머스 백엔드. Gradle 멀티 모듈이며, PG 시뮬레이터 앱은 Kotlin으로 구현한다.

## 모듈 구조

- `apps/commerce-api`: 실행 진입점 + 5계층 코드
- `apps/pg-simulator`: 로컬 결제 연동 검증용 Kotlin Spring Boot 앱. 현재 `commerce-api`의 기본 `PaymentGateway` 구현에는 아직 연결되지 않은 별도 시뮬레이터다.
- `modules/{jpa,redis,kafka}`: 인프라 베이스 (JPA DataSource, Redis ConnectionFactory/RedisTemplate, Kafka 의존성 베이스)
- `supports/{jackson,logging,monitoring}`: 횡단 관심사

베이스 패키지 `com.commerce`를 공유하고, 모듈마다 서브패키지로 분리한다.

## DDD 핵심 원칙 (요약)

상세 규칙과 예시는 `.agents/rules/ddd.md`를 참조한다.
아래 5계층 DDD 규칙은 `apps/commerce-api`에 적용한다. `apps/pg-simulator`는 로컬 결제 검증용 Kotlin 시뮬레이터이며, 현재 JPA 엔티티를 `com.commerce.pg.domain`에 두는 별도 구조다. `commerce-api` 주문 흐름은 아직 이 앱을 호출하지 않고 `StubPaymentGateway`를 사용한다.

### 5계층 + 의존성 방향

```text
interfaces -> application -> domain <- infrastructure
                                   ^
                              support (어디서든)
```

- `interfaces`: Controller, Request/Response (HTTP 경계)
- `application`: UseCase, Info (**트랜잭션 경계**)
- `domain`: 순수 자바. **JPA 어노테이션 금지, 다른 계층 import 금지**
- `infrastructure`: JpaEntity, JpaRepository, RepositoryImpl
- `support`: CoreException, ErrorType 등 횡단 보조

### 절대 규칙

- 도메인 모델과 JPA 엔티티는 **별도 클래스**로 분리한다.
- JPA 엔티티 연관관계 어노테이션(`@ManyToOne` 등)은 **전면 금지**한다. 상대 객체는 ID로만 참조한다. 단, Aggregate 내부 값 컬렉션의 `@CollectionTable` 소유자 FK 지정은 예외다.
- 기본은 1 트랜잭션 = 1 Aggregate 수정이다. 다만 강한 정합성이 필요한 흐름(product 등록의 Product+SKU 초기 생성, order 생성·취소 보상, coupon 발급·사용처럼 문서에 "의식적 규칙 예외"로 남긴 경우)은 application 계층에서 `@Transactional` 또는 `TransactionTemplate`으로 트랜잭션 경계를 명시한다.
- Controller는 UseCase만 호출한다. Domain Service를 직접 호출하지 않는다.
- 비즈니스 용어 = 코드 명칭 (유비쿼터스 언어).

## 규칙 인덱스

- 작업 방식: `.agents/rules/behavior.md` (**세션 시작 시 필수**)
- DDD 코딩: `.agents/rules/ddd.md` (Java 작업 전 필수)
- 커밋: `.agents/rules/commit.md` (`/commit` 명령으로 자동 적용)
