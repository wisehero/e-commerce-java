# 커밋 규칙

## 형식
```
<type>(<scope>): <subject>

<body (선택)>

<footer (선택)>
```

## type (영문 고정)
- `feat`: 새 기능
- `fix`: 버그 수정
- `refactor`: 기능 변화 없는 구조 개선
- `perf`: 성능 개선
- `wiki`: 문서 (wiki·README·AGENTS.md·CLAUDE.md·주석)
- `test`: 테스트 코드 추가·수정
- `build`: Gradle·Docker·CI 설정
- `chore`: 의존성 업데이트, 기타 잡일
- `style`: 포맷팅·세미콜론 등 (로직 무변경)
- `init`: 모듈·기능의 초기 셋업

## scope (도메인 범위)
- 도메인명: `member`, `order`, `product`, `cart`, `coupon`, `brand`, `category`
- 앱 모듈: `commerce-api`, `pg-simulator`
- 인프라 모듈: `jpa`, `redis`, `kafka`
- 횡단 관심사: `jackson`, `logging`, `monitoring`
- 전 영역 변경: `project`
- 단일 범위로 묶이지 않으면 가장 큰 영향 범위 하나만 선택 (여러 scope 나열 금지)

## subject
- 한국어, 50자 이내, 명령형, 마침표 없음
- 예: "회원가입 API 추가" (O) / "회원가입 API를 추가했습니다." (X)

## body (선택)
- 72자에서 줄바꿈
- **왜 바꿨는지(why)** 위주. what은 diff에서 보임
- 한 줄 비우고 시작

## footer (선택)
- 이슈 참조: `Refs: #12`
- Breaking Change: `BREAKING CHANGE: ...`
- `Co-Authored-By` 트레일러 추가 금지

## 규칙
- 한 커밋은 한 가지 의도만. 무관한 변경 섞지 않음.
- DDD 5계층 중 여러 계층 관통한 변경은 도메인명으로 묶기 (`feat(member)`).
- 인프라 모듈 단독 변경은 모듈명 (`build(jpa)`).

## 예시
```
feat(member): 회원가입 API 및 도메인 모델 추가

Member 도메인 모델과 MemberJpaEntity를 분리하고
이메일 중복 검증을 UseCase에 위치시킴.

Refs: #3
```

```
refactor(order): Order 도메인을 JPA 엔티티에서 분리

도메인 테스트를 Spring 없이 돌리기 위해 순수 자바 모델로 추출하고
OrderJpaEntity로 영속 매핑을 격리.
```

```
build(project): Gradle 9.0.0 래퍼 적용
```
