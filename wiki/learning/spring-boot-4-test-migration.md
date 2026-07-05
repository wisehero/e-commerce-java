# Spring Boot 4 — 테스트 인프라 마이그레이션 노트

본 프로젝트가 Spring Boot 4.0.3을 도입하면서 부딪힌 테스트 관련 변경점.
Spring Boot 3 기준의 관행을 그대로 가져오면 **컴파일 또는 런타임에서 깨진다**.

## 1. `@MockBean` → `@MockitoBean`

### 변화

| Spring Boot 3 | Spring Boot 4 |
|---|---|
| `org.springframework.boot.test.mock.mockito.MockBean` | `org.springframework.test.context.bean.override.mockito.MockitoBean` |

`@MockBean`은 Spring Boot 3.4에서 deprecated되고 Spring Boot 4에서 **제거**되었다.
대체는 Spring Test 6.2+가 제공하는 `@MockitoBean` (`spring-test` 모듈 소속).

### 사용 예

```java
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(MemberControllerV1.class)
class MemberControllerV1Test {

    @MockitoBean                                  // ← @MockBean 아님
    private MemberSignUpUseCase memberSignUpUseCase;
    // ...
}
```

`@MockitoSpyBean`도 함께 도입되어 기존 `@SpyBean`을 대체한다.

## 2. `@WebMvcTest` 패키지 이동

### 변화

| Spring Boot 3 | Spring Boot 4 |
|---|---|
| `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` | `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` |

`@WebMvcTest`가 `spring-boot-webmvc-test` **별도 모듈로 분리**되고 패키지 경로도 함께 바뀌었다.

### 사용 예

```java
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;  // ← 새 패키지

@WebMvcTest(MemberControllerV1.class)
class MemberControllerV1Test { ... }
```

## 3. `spring-boot-webmvc-test` 모듈 명시 의존

### 변화

Spring Boot 3까지는 `spring-boot-starter-test`만 추가하면 `@WebMvcTest`, `@DataJpaTest` 같은 슬라이스 어노테이션이 자동으로 따라왔다.
Spring Boot 4부터는 슬라이스 테스트 모듈이 **각 기술 모듈로 분리**되었고, `spring-boot-starter-test`가 자동으로 끌어오지 **않는다**.

### 대응

`apps/commerce-api/build.gradle.kts` (또는 해당 모듈) — 슬라이스 테스트가 필요한 곳에만 추가:

```kotlin
dependencies {
    // ... 기존 ...
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
}
```

BOM(`spring-boot-dependencies`)이 버전을 잡아주므로 버전 명시 불필요.

### 증상

추가 전 컴파일 실패 메시지:
```
package org.springframework.boot.webmvc.test.autoconfigure does not exist
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
```

`@DataJpaTest`, `@DataRedisTest` 등 다른 슬라이스도 각자 별도 모듈(`spring-boot-jpa-test`, `spring-boot-data-redis-test` 등)로 분리됐을 가능성이 높다.
필요할 때 빌드 에러 메시지를 따라가 해당 모듈을 추가한다.

## 4. Jackson 3 — `tools.jackson.*` 패키지

### 변화

Spring Boot 4는 Jackson 3을 디폴트로 사용한다. Jackson 3은 패키지 자체가 변경되었다.

| Jackson 2 | Jackson 3 |
|---|---|
| `com.fasterxml.jackson.databind.ObjectMapper` | `tools.jackson.databind.ObjectMapper` |
| `com.fasterxml.jackson.databind.DeserializationFeature` | `tools.jackson.databind.DeserializationFeature` |
| `com.fasterxml.jackson.databind.exc.InvalidFormatException` | `tools.jackson.databind.exc.InvalidFormatException` |

`@JsonInclude` 같은 어노테이션은 여전히 `com.fasterxml.jackson.annotation.*`에 있다 (Jackson Annotations 패키지는 별도 모듈).

### 사용 예 (테스트에서 ObjectMapper 주입)

```java
import tools.jackson.databind.ObjectMapper;       // ← Jackson 3 패키지

@Autowired
private ObjectMapper objectMapper;

// writeValueAsString은 그대로 사용 가능
String json = objectMapper.writeValueAsString(request);
```

### 본 프로젝트 사례

- `supports/jackson/src/main/java/com/commerce/config/jackson/JacksonConfig.java` — `tools.jackson.*` 임포트
- `interfaces/api/ApiControllerAdvice.java` — `tools.jackson.databind.exc.InvalidFormatException` 임포트
- 일부 enum 관련 feature도 위치가 바뀌었다: `DeserializationFeature.READ_ENUMS_USING_TO_STRING` → `EnumFeature.READ_ENUMS_USING_TO_STRING`

## 5. Bean Validation — `runtimeOnly` 함정

### 증상

```kotlin
// 잘못된 설정
"runtimeOnly"("org.springframework.boot:spring-boot-starter-validation")
```

→ `jakarta.validation.constraints.NotBlank`, `jakarta.validation.Valid` 같은 어노테이션 import가 **컴파일 타임에 실패**한다.

### 원인

`spring-boot-starter-validation`은 두 가지를 함께 끌어온다:

- `jakarta.validation:jakarta.validation-api` — **어노테이션 API**, 컴파일 타임에 필요
- `org.hibernate.validator:hibernate-validator` — 구현체, 런타임에만 있어도 됨

`runtimeOnly`로 두면 컴파일 클래스패스에 API가 없어 어노테이션을 못 쓴다.

### 대응

```kotlin
"implementation"("org.springframework.boot:spring-boot-starter-validation")
```

`implementation`으로 두면 API는 컴파일 타임에 보이고, 구현체는 자동으로 런타임에 등록된다 (Spring Boot가 `LocalValidatorFactoryBean` 자동 구성).

## 6. 본 프로젝트에서 검증된 슬라이스 테스트 골격

회원가입 슬라이스 테스트(`MemberControllerV1Test`)에서 실제로 통과한 형태:

```java
package com.commerce.interfaces.api.member;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.commerce.application.member.MemberSignUpUseCase;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(MemberControllerV1.class)
class MemberControllerV1Test {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private MemberSignUpUseCase memberSignUpUseCase;

    @Test
    void should_returnApiResponseSuccess_when_validRequest() throws Exception {
        // given / when / then ...
    }
}
```

핵심 차이를 한 화면에서 확인 가능:
- `@MockitoBean` (not `@MockBean`)
- `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`
- `tools.jackson.databind.ObjectMapper`

## 7. 트러블슈팅 빠른 참조

| 증상 | 원인 | 대응 |
|---|---|---|
| `package org.springframework.boot.test.autoconfigure.web.servlet does not exist` | `@WebMvcTest` 옛 패키지 사용 | 새 패키지 `org.springframework.boot.webmvc.test.autoconfigure`로 변경 + `spring-boot-webmvc-test` 모듈 추가 |
| `cannot find symbol: class MockBean` | `@MockBean` 제거됨 | `@MockitoBean`으로 교체, import는 `org.springframework.test.context.bean.override.mockito.*` |
| `package jakarta.validation.constraints does not exist` | validation starter가 `runtimeOnly` | `implementation`으로 스코프 변경 |
| `package com.fasterxml.jackson.databind does not exist` (Spring Boot 4 환경에서) | Jackson 3 사용 중 | `tools.jackson.*` 패키지로 import 경로 변경 |

## 출처 / 참고

- Spring Boot 4.0 Release Notes — https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes
- Spring Framework 6.2: `@MockitoBean`, `@MockitoSpyBean` — https://docs.spring.io/spring-framework/reference/testing/annotations/integration-spring/annotation-mockitobean.html
- Jackson 3 패키지 변경 (`com.fasterxml.jackson` → `tools.jackson`) — https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0

> 위 정보는 본 프로젝트(Spring Boot 4.0.3, JDK 25)에서 직접 부딪혀 검증한 사례 기반.
> Spring Boot 4 마이너 버전이 올라가며 모듈 분할이 더 변경될 수 있으니, 새 슬라이스 어노테이션(`@DataJpaTest` 등)을 처음 쓸 때는 빌드 에러를 따라가 모듈 추가 여부를 먼저 확인할 것.
