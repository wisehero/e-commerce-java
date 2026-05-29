# AGENTS.md

## 최우선

**세션 시작 시 `.agents/rules/behavior.md`를 반드시 읽는다.** 파일 수정 권한, 제안 형식, 대화 방식 등 모든 작업 방식이 거기에 정의되어 있다.

## 프로젝트 개요

Java 25 + Spring Boot 4 기반 이커머스 백엔드. Gradle 멀티 모듈.

## 모듈 구조

- `apps/commerce-api`: 실행 진입점 + 5계층 코드
- `modules/{jpa,redis,kafka}`: 인프라 베이스 (DataSource, ConnectionFactory, KafkaTemplate)
- `supports/{jackson,logging,monitoring}`: 횡단 관심사

베이스 패키지 `com.commerce`를 공유하고, 모듈마다 서브패키지로 분리한다.

## DDD 핵심 원칙 (요약)

상세 규칙과 예시는 `.agents/rules/ddd.md`를 참조한다.

### 5계층 + 의존성 방향

```text
interfaces -> application -> domain <- infrastructure
                                   ^
                              support (어디서든)
```

- `interfaces`: Controller, Dto (HTTP 경계)
- `application`: UseCase, Info (**트랜잭션 경계**)
- `domain`: 순수 자바. **JPA 어노테이션 금지, 다른 계층 import 금지**
- `infrastructure`: JpaEntity, JpaRepository, RepositoryImpl
- `support`: CoreException, ErrorType 등 횡단 보조

### 절대 규칙

- 도메인 모델과 JPA 엔티티는 **별도 클래스**로 분리한다.
- JPA 연관관계 어노테이션(`@ManyToOne` 등)은 **전면 금지**한다. 상대 객체는 ID로만 참조한다.
- 1 트랜잭션 = 1 Aggregate 수정. `@Transactional`은 application UseCase에 둔다.
- Controller는 UseCase만 호출한다. Domain Service를 직접 호출하지 않는다.
- 비즈니스 용어 = 코드 명칭 (유비쿼터스 언어).

## 규칙 인덱스

- 작업 방식: `.agents/rules/behavior.md` (**세션 시작 시 필수**)
- DDD 코딩: `.agents/rules/ddd.md` (Java 작업 전 필수)
- 커밋: `.agents/rules/commit.md` (`/commit` 명령으로 자동 적용)
