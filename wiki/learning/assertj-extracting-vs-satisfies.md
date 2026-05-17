# AssertJ — `extracting` vs `satisfies`

테스트 코드에서 객체의 여러 필드를 검증할 때 자주 헷갈리는 두 메서드의 차이와 사용 기준.

## 한 줄 요약

> **`extracting`은 검증 컨텍스트를 추출된 값으로 "전환"하고, `satisfies`는 원본 객체에 "유지"한다.**

## 본질 차이

| | `extracting` | `satisfies` |
|---|---|---|
| 컨텍스트 | 추출된 값으로 **전환** | 원본 객체에 **유지** |
| 반환 타입 | 추출 값의 AssertJ assertion 객체 (`ObjectAssert<R>`, `StringAssert`, `ListAssert` 등) | 원본 assertion 객체 (`this`) — 계속 체이닝 가능 |
| 강타입성 | `extracting("필드명")` 문자열 형태는 컴파일 타임 검증 불가. `extracting(T::getter)` 메서드 레퍼런스 형태는 강타입 | 람다 안에서 직접 메서드 호출 — 항상 강타입 |
| 실패 메시지 | "extracted 'errorType' from ..." 식으로 추출 경로 자동 표기 | 람다 안 단언이 그대로 노출 (추출 경로는 없음) |

## 시그니처 변형

```java
// extracting
.extracting("fieldName")                    // String — 컴파일 타임 검증 불가
.extracting(T::getter)                      // Function — 강타입
.extracting("a", "b", "c")                  // tuple — 여러 필드 동시 추출
.extracting(T::getter, InstanceOfAssertFactories.STRING)  // 강타입 + 특화 assertion

// satisfies
.satisfies(t -> assertThat(t.getX()).isEqualTo(...))    // Consumer<T>
.satisfies(t -> { ... 여러 단언 ... })                  // 한 람다에 묶기
.satisfiesAnyOf(c1, c2, c3)                             // 하나만 만족
```

## 코드 비교

```java
// extracting — 추출된 값 위에서 검증
assertThatThrownBy(() -> new Email("abc"))
    .isInstanceOf(CoreException.class)
    .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);
// 이 체인 이후로는 컨텍스트가 errorType(ObjectAssert)이라
// Email/CoreException 단언으로 못 돌아간다.

// satisfies — 원본에 머물러 계속 체이닝
assertThat(member)
    .satisfies(m -> assertThat(m.getId()).isNull())
    .satisfies(m -> assertThat(m.getEmail()).isEqualTo(VALID_EMAIL))
    .satisfies(m -> assertThat(m.getNickname()).isEqualTo(VALID_NICKNAME));
// 매 줄 컨텍스트가 member로 돌아옴. 어떤 메서드든 자유롭게.
```

## 언제 무엇을 쓰나

| 상황 | 권장 |
|---|---|
| **한 필드만** 깊게 검증 (예: 예외의 `errorType`만 확인) | `extracting` — 한 줄로 간결 |
| **여러 필드**를 **각각 다른 방식**으로 검증 (some `isNull`, some `isEqualTo`, some `isGreaterThan`) | `satisfies` 체인 — 가독성 좋음 |
| **여러 필드**를 **모두 `isEqualTo`** 로 한 번에 비교 | `usingRecursiveComparison().isEqualTo(expected)` 가 최고. 안 되면 `extracting(...).containsExactly(...)` tuple |
| **타입 안전성**이 중요 (리팩토링 시 컴파일러 도움) | `satisfies` 또는 `extracting(T::getter)` 메서드 레퍼런스 형태 |
| **컬렉션의 각 원소**를 검증 | `extracting(T::field).contains(...)` 또는 `allSatisfy(...)` |

## 안티패턴

```java
// (1) extracting 문자열 형태로 깊게 — 리팩토링 시 사일런트 깨짐
assertThat(member).extracting("email.value").isEqualTo("user@example.com");
// → satisfies(m -> assertThat(m.getEmail().value()).isEqualTo(...)) 가 안전

// (2) usingRecursiveComparison 가능한데 satisfies 5개 체이닝
assertThat(member)
    .satisfies(m -> assertThat(m.getA()).isEqualTo(a))
    .satisfies(m -> assertThat(m.getB()).isEqualTo(b))
    .satisfies(m -> assertThat(m.getC()).isEqualTo(c))
    .satisfies(m -> assertThat(m.getD()).isEqualTo(d))
    .satisfies(m -> assertThat(m.getE()).isEqualTo(e));
// → usingRecursiveComparison().isEqualTo(expected) 한 줄이 명료

// (3) 단일 chained 표현 강박으로 가독성 손해
// AssertJ의 "한 표현 원칙"이 강박이 되면 분리된 5줄이 더 읽힐 때가 있다.
// 의도 우선, 표현 통합은 그 다음.
```

## 본 프로젝트 사례

- `MemberSignUpFacadeTest.should_returnMemberInfo_when_signUpWithValidCommand`
  - `MemberInfo`(record)의 모든 필드 비교 → `usingRecursiveComparison()`이 최적
- 예외 케이스 (`extracting("errorType")`)
  - 단일 필드 검증이라 `extracting`이 간결
- `MemberTest.should_createWithoutId_andUserRole_when_register`
  - `Member.register(...)`가 invariant 통과해야 만들어지므로 "기대 Member 인스턴스" 생성이 번거로움
  - → `satisfies` 체인 또는 5줄 분리 단언이 현실적 선택

## 결론

`extracting`은 **"이 값 하나"** 에 집중, `satisfies`는 **"원본 위에서 자유로운 검증"**.
무조건 한 줄로 합치려 하지 말고, **의도가 가장 잘 드러나는 형태**를 고른다.
