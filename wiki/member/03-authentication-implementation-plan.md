# 회원 인증·인가 구현 계획

작성: 2026-07-04 · 상태: 단계별 구현 진행 중

현재 코드 기준으로 1단계의 보안 의존성·기본 경계, 2단계의 회원 등급·상태·실패 횟수·인증 버전 모델링, 7단계의 기존 고객 API memberId 제거와 관리자 경로 분리는 완료되어 있다.
3~6단계의 refresh session, token issuer, Auth API, 인증 스냅샷 캐시, rate limit, 감사 로그는 아직 구현되지 않았다.

## 1. 목표

운영 수준의 회원 인증·인가를 구현한다.

기준 결정은 [`02-authentication-authorization-adr.md`](./02-authentication-authorization-adr.md)를 따른다.

핵심 목표는 다음과 같다.

- 이메일과 비밀번호로 로그인한다.
- 비밀번호는 단방향 해시로만 저장한다.
- access JWT와 refresh token rotation을 사용한다.
- 자동로그인은 refresh token 만료 기간 차이로 처리한다.
- 로그인 실패 5회 시 계정을 `LOCKED` 상태로 전환한다.
- 회원 상태·권한·비밀번호 변경·전체 로그아웃 시 `authVersion`을 증가시켜 기존 access token을 무효화한다.
- 인증된 회원 ID를 기준으로 내 리소스에 접근한다.
- 관리자 API는 `ADMIN` 권한으로 제한한다.
- 로그인 성공·실패·잠금·잠금 해제·토큰 재사용 감지는 감사 로그로 남긴다.

## 2. 현재 코드 검증 결과

계획 작성 시점에 다음을 확인했다.

| 검증 항목 | 결과 | 영향 |
|---|---|---|
| Spring Security web 의존성 | 있음. `spring-boot-starter-security`, OAuth2 Resource Server 의존성과 기본 `SecurityFilterChain` 존재 | JWT 검증·로그인/refresh 실제 흐름은 후속 구현 필요 |
| 비밀번호 해시 | `PasswordHasher` 포트와 `BcryptPasswordHasher` 구현 존재 | 기존 포트를 유지하고 cost 설정화만 추가 |
| Redis | `modules:redis`와 `RedisTemplate<String, String>` 구성 존재 | 인증 스냅샷, denylist, rate limit 저장소로 활용 가능 |
| 회원 상태 | `MemberStatus` 존재 | 로그인/refresh 흐름에서 상태 검증 연결 필요 |
| 로그인 실패 횟수 | 필드 존재 | 로그인 실패 유스케이스 연결 필요 |
| 인증 버전 | `authVersion` 존재 | access token 무효화 검증 연결 필요 |
| refresh token 저장소 | 없음 | 신규 auth 세션 Aggregate 또는 저장 포트 필요 |
| 외부 memberId 입력 | cart, order, coupon 고객 API에서 제거됨 | interfaces 계층이 인증 principal의 memberId를 command에 주입 |
| 계층 규칙 | application은 infrastructure를 직접 참조할 수 없음 | token/cache/rate-limit은 domain port + infrastructure adapter로 둔다 |

## 3. 구현 원칙

- domain은 Spring Security, JWT, Redis, JPA를 모른다.
- interfaces는 인증된 사용자 정보를 application command로 변환한다.
- application이 인증 흐름과 트랜잭션 경계를 가진다.
- infrastructure가 JWT 서명, refresh token 저장, Redis 캐시, rate limit을 구현한다.
- JPA 엔티티 연관관계 어노테이션은 사용하지 않는다.
- 클라이언트가 전달한 `memberId`는 내 리소스 접근 판단에 사용하지 않는다.

## 4. 단계별 구현 계획과 검증

### 1단계: 보안 의존성과 기본 설정 추가

상태: 2026-07-04 기준 완료. 단, `/api/v1/auth/login`과 `/api/v1/auth/refresh`는 공개 경로로 예약되어 있을 뿐 실제 Controller는 아직 없다.

작업:

- `spring-boot-starter-security` 추가
- JWT 검증을 위한 OAuth2 Resource Server 또는 Nimbus JOSE/JWT 의존성 추가
- 인증 설정 properties 추가
  - issuer
  - access token TTL
  - refresh token TTL
  - remember-me refresh token TTL
  - signing key 또는 key pair 경로
- 기본 `SecurityFilterChain` 추가
- 공개 API와 보호 API 경계 정의

초기 공개 API:

- `POST /api/v1/members`
- `POST /api/v1/auth/login` (공개 경로로 예약, Controller는 아직 없음)
- `POST /api/v1/auth/refresh` (공개 경로로 예약, Controller는 아직 없음)
- Swagger/OpenAPI 경로
- actuator health 경로가 있다면 health만 공개

검증:

- `./gradlew :apps:commerce-api:compileJava`
- `./gradlew :apps:commerce-api:test --tests 'com.commerce.architecture.*'`
- 보안 설정 테스트
  - 인증 없이 보호 API 접근 시 401
  - 공개 API는 인증 없이 접근 가능
  - 권한 부족 시 403

### 2단계: Member 상태와 인증 버전 모델링

상태: 2026-07-04 기준 완료. `MemberGrade`, `MemberStatus`, `loginFailureCount`, `authVersion` 필드와 JPA 매핑, 도메인 테스트·영속성 테스트가 존재한다.

작업:

- `MemberStatus` 추가
  - `ACTIVE`
  - `LOCKED`
  - `SUSPENDED`
  - `WITHDRAWN`
- `Member`에 필드 추가
  - `grade`
  - `status`
  - `loginFailureCount`
  - `authVersion`
- 도메인 메서드 추가
  - `recordLoginFailure()`
  - `resetLoginFailures()`
  - `lockByLoginFailures()` private helper
  - `suspend()`
  - `withdraw()`
  - `unlock()`
  - `increaseAuthVersion()`
  - `ensureLoginAllowed()`
  - `changeGrade(MemberGrade)`
- 신규 가입 회원은 `BRONZE`, `ACTIVE`, 실패 횟수 0, 인증 버전 1로 시작
- `MemberJpaEntity`와 repository 매핑 갱신

검증:

- domain 테스트
  - 신규 회원 기본 상태 검증
  - 로그인 실패 4회까지는 `ACTIVE`
  - 5회 실패 시 `LOCKED`
  - `LOCKED`, `SUSPENDED`, `WITHDRAWN`은 로그인 불가
  - 로그인 성공 시 실패 횟수 초기화
  - 상태 변경 시 `authVersion` 증가
- persistence 테스트
  - `grade`, `status`, `loginFailureCount`, `authVersion` 저장·복원
- `./gradlew :apps:commerce-api:test --tests 'com.commerce.domain.member.*'`
- `./gradlew :apps:commerce-api:test --tests 'com.commerce.infrastructure.member.*'`

### 3단계: 인증 토큰과 refresh session 저장소 구현

작업:

- domain auth 패키지 추가 검토
  - `RefreshSession`
  - `RefreshSessionRepository`
  - `TokenIssuer`
  - `RefreshTokenHasher`
  - `AuthSessionId` 또는 primitive ID 정책
- refresh session 저장 필드
  - id
  - memberId
  - sessionId
  - tokenFamilyId
  - refreshTokenHash
  - status
  - rememberMe
  - issuedAt
  - expiresAt
  - rotatedAt
  - revokedAt
  - lastUsedAt
  - userAgentHash optional
  - ipAddressHash optional
- refresh token 원문은 DB에 저장하지 않음
- refresh token 재사용 감지 시 같은 token family 폐기
- token 발급·검증 구현은 infrastructure에 둔다

검증:

- refresh session domain 테스트
  - 활성 세션만 회전 가능
  - 만료 세션 회전 거부
  - 폐기 세션 회전 거부
  - 재사용 감지 시 family 폐기 대상 식별
- repository 통합 테스트
  - hash 저장 확인
  - memberId/sessionId/familyId 기준 조회
  - family 단위 폐기
- token issuer 테스트
  - JWT claim에 `sub`, `role`, `authVersion`, `sid`, `jti`, `iat`, `exp` 포함
  - 만료 token 검증 실패
  - 잘못된 서명 검증 실패

### 4단계: 로그인/refresh/logout/me API 구현

작업:

- interfaces
  - `AuthControllerV1`
  - `LoginRequest`, `RefreshRequest`, `LogoutRequest`
  - `TokenResponse`, `MeResponse`
- application
  - `AuthLoginUseCase`
  - `AuthRefreshUseCase`
  - `AuthLogoutUseCase`
  - `AuthMeUseCase`
- 로그인 흐름
  - 이메일로 회원 조회
  - 존재하지 않는 이메일과 비밀번호 불일치는 동일한 에러 응답
  - `ACTIVE`가 아니면 로그인 거부
  - 비밀번호 불일치 시 실패 횟수 증가
  - 실패 5회 시 `LOCKED`, `authVersion` 증가, refresh token 폐기
  - 성공 시 실패 횟수 초기화, token 발급
- refresh 흐름
  - refresh token hash 조회
  - session 상태·만료 확인
  - member 상태·authVersion 확인
  - 기존 refresh token 회전
  - 새 access/refresh token 발급
- logout 흐름
  - 현재 refresh session 폐기
- me 흐름
  - 인증된 memberId 기준 회원 정보 반환

검증:

- application 테스트
  - 로그인 성공 시 token 발급
  - 비밀번호 불일치 5회 시 `LOCKED`
  - 잠긴 회원 로그인 거부
  - 정지·탈퇴 회원 로그인 거부
  - 로그인 성공 시 실패 횟수 초기화
  - refresh 성공 시 refresh token rotation
  - 재사용 refresh token 감지 시 family 폐기
  - 로그아웃 후 refresh 거부
- controller 테스트
  - 요청 validation
  - 로그인 실패 응답이 이메일 없음/비밀번호 틀림을 구분하지 않음
  - refresh/logout/me HTTP 계약 검증
- `./gradlew :apps:commerce-api:test --tests 'com.commerce.application.auth.*'`
- `./gradlew :apps:commerce-api:test --tests 'com.commerce.interfaces.api.auth.*'`

### 5단계: 인증 스냅샷 캐시와 access token 무효화

작업:

- `AuthenticatedMemberSnapshot` 모델 추가
  - memberId
  - role
  - status
  - authVersion
- Redis 캐시 adapter 구현
- Security filter 또는 authentication converter에서 다음 검증
  - JWT 서명과 만료
  - 캐시 또는 DB의 회원 스냅샷 조회
  - `status == ACTIVE`
  - JWT `authVersion == current authVersion`
- 상태·권한·비밀번호 변경, 잠금, 탈퇴, 전체 로그아웃 시 캐시 무효화
- 즉시 차단이 필요한 access token은 `sid` 또는 `jti` denylist를 만료 시각까지 저장

검증:

- security 통합 테스트
  - 정상 token은 보호 API 접근 가능
  - `authVersion`이 오래된 token은 401
  - `LOCKED`/`SUSPENDED`/`WITHDRAWN` 회원 token은 401
  - denylist에 등록된 `sid` 또는 `jti`는 401
- Redis adapter 테스트
  - TTL 설정
  - 캐시 miss 시 DB fallback
  - 캐시 무효화

### 6단계: 로그인 실패 rate limit과 감사 로그

작업:

- rate limit port 추가
  - 이메일 기준
  - IP 기준
  - 이메일+IP 조합 기준
- Redis 기반 rate limit adapter 구현
- 로그인 성공·실패·잠금·잠금 해제·refresh 재사용 감지 감사 로그 추가
- 감사 로그는 비밀번호, token 원문, Authorization header를 남기지 않음
- 구조화 로그 필드 정의
  - eventType
  - memberId optional
  - emailHash
  - ipHash
  - userAgentHash
  - result
  - reason

검증:

- rate limit 테스트
  - 임계치 이하 허용
  - 임계치 초과 차단
  - TTL 이후 재시도 가능
- 감사 로그 테스트 또는 appender 기반 검증
  - token/password 원문 미포함
  - eventType/result/reason 포함

### 7단계: 기존 API의 memberId 제거와 인가 적용

상태: 2026-07-04 기준 기존 고객 API 경계 정리 구현 완료.

작업:

- cart API
  - 요청 body/query의 `memberId` 제거 완료
  - 인증된 memberId를 command에 주입 완료
  - checkout도 인증된 memberId 기준으로 수행
- order API
  - 주문 생성 요청의 `memberId` 제거 완료
  - 기존 `GET /api/v1/orders`를 내 주문 목록으로 유지하고 인증 회원 기준으로 전환
  - 주문 상세 조회와 주문 취소는 본인 주문인지 확인
  - 다른 회원 주문은 현재 ErrorType 정책과 리소스 은닉 목적에 맞춰 `NOT_FOUND`로 응답
- coupon API
  - 쿠폰 발급은 요청 body의 `memberId`를 제거하고 인증 회원 기준 적용
  - 회원 쿠폰 조회는 `/api/v1/coupons/me`로 전환
  - 주문 쿠폰 사용은 인증 memberId가 `OrderPlaceCommand`로 들어가 본인 쿠폰만 사용하도록 검증
- admin API
  - 상품 등록/상태 변경, SKU 할인/가격/재고 변경, 브랜드 등록/수정/상태 변경, 카테고리 등록/수정/상태 변경/삭제, 쿠폰 정책 생성을 `/api/v1/admin/**` 경로로 분리
  - `/api/v1/admin/**`는 `ADMIN` 권한만 허용
- 기존 memberId 기반 고객 API는 별도 deprecated 경로 없이 제거

검증:

- controller 테스트 갱신
  - body/query memberId 없이 동작
  - 다른 회원 리소스 접근 불가
  - USER가 `/api/v1/admin/**` 접근 시 403
  - ADMIN이 `/api/v1/admin/**` 접근 가능
- application 테스트
  - 주문 상세/취소 소유권 검증
  - 본인 쿠폰만 주문에 사용 가능
  - 카트는 인증 회원의 카트만 조회·변경
- ArchUnit
  - interfaces가 domain/infrastructure를 직접 참조하지 않음
  - application이 security/infrastructure 구현체를 직접 참조하지 않음

### 8단계: 운영 준비와 문서화

작업:

- application.yml 설정 문서화
- 운영 secret 주입 방식 정리
- key rotation 운영 절차 문서화
- 토큰 만료 시간과 rate limit 값 문서화
- 장애 시나리오 정리
  - Redis 장애
  - DB 장애
  - JWT signing key 교체
  - refresh token 재사용 감지
- OpenAPI 문서 갱신
- migration 주의사항 작성

검증:

- 전체 테스트
  - `./gradlew :apps:commerce-api:test`
- API smoke test
  - 회원가입
  - 로그인
  - me
  - cart 조회
  - refresh
  - logout
  - refresh 재사용 거부
- 로그 점검
  - password/token 원문 미출력
- 문서 점검
  - `wiki/member/01-member-policy.md`
  - `wiki/member/02-authentication-authorization-adr.md`
  - 이 문서

## 5. 구현 순서 추천

추천 순서는 다음과 같다.

1. Member 상태·실패 횟수·authVersion 도메인 먼저 구현
2. refresh session 저장소와 token issuer 구현
3. 로그인·refresh·logout API 구현
4. SecurityFilterChain과 인증 스냅샷 검증 연결
5. cart API부터 memberId 제거 (완료)
6. order, coupon API로 memberId 제거 확대 (완료)
7. admin API를 `/api/v1/admin/**`로 분리하고 권한 적용 (완료)
8. 감사 로그와 rate limit 강화

이 순서를 추천하는 이유는 인증 도메인 상태가 먼저 안정되어야 token과 API 경계를 안전하게 얹을 수 있기 때문이다.
또한 memberId 제거는 API 계약 변화가 크므로 cart처럼 범위가 명확한 도메인부터 점진적으로 전환한다.

## 6. 완료 기준

다음 조건을 만족하면 인증·인가 1차 구현을 완료로 본다.

- 이메일/비밀번호 로그인이 가능하다.
- refresh token rotation이 동작한다.
- 자동로그인 요청 시 refresh token 만료 기간이 길어진다.
- 비밀번호 불일치 5회 시 회원이 `LOCKED`가 된다.
- `LOCKED`, `SUSPENDED`, `WITHDRAWN` 회원은 로그인과 refresh가 불가능하다.
- 상태·권한·비밀번호 변경 후 기존 access token이 거부된다.
- 내 리소스 API는 인증된 회원 기준으로만 동작한다.
- 관리자 API는 `ADMIN`만 접근할 수 있다.
- token/password 원문이 로그에 남지 않는다.
- 전체 테스트와 아키텍처 테스트가 통과한다.

## 7. 이번 계획 검증 수행 내역

계획 작성과 동시에 다음 검증을 수행했다.

- `apps/commerce-api/build.gradle.kts` 확인
  - 현재 `spring-boot-starter-security`, OAuth2 Resource Server, `spring-security-crypto` 의존성이 있음
- `modules/redis` 설정 확인
  - `RedisTemplate<String, String>` 사용 가능
- 회원 도메인/JPA 확인
  - `MemberGrade`, `MemberStatus`, `loginFailureCount`, `authVersion` 필드 존재
- `memberId` 외부 입력 범위 확인
  - cart, order, coupon 고객 API는 요청 body/query/path의 `memberId`를 사용하지 않음
  - interfaces 계층이 인증 principal의 memberId를 application command로 전달
- 공개/보호/관리자 API 경계 확인
  - 상품 목록/상세, 브랜드 상세, 카테고리 트리/상세, Swagger/OpenAPI, actuator health, 회원가입, 로그인/refresh 예정 경로는 공개
  - cart/order/coupon 고객 API는 인증 필요
  - 상품·SKU·브랜드·카테고리·쿠폰 정책 관리 API는 `/api/v1/admin/**`로 분리하고 `ADMIN`만 허용
- 아키텍처 룰 확인
  - application은 infrastructure를 직접 참조하면 안 되므로 token/cache/rate-limit은 port/adapter로 분리 필요

초기 계획 작성 당시에는 코드 테스트를 실행하지 않았다.
이후 API 경계 정리 구현에서는 관련 controller/security/application 테스트를 추가·갱신했다.
