## PG-Simulator (PaymentGateway)

### Description
로컬 개발과 결제 연동 검증을 위해 PaymentGateway 를 시뮬레이션하는 App Module 입니다.
`local` 프로필로 실행 권장하며, 커머스 서비스와의 동시 실행이 가능하도록 서버 포트가 조정되어 있습니다.
현재 `apps/commerce-api`의 기본 주문 흐름은 `StubPaymentGateway`를 사용하며, 이 앱을 자동 호출하는 HTTP 어댑터는 아직 없습니다.
따라서 이 앱은 결제 API 자체를 수동으로 검증하거나, 이후 `PaymentGateway` 어댑터를 붙일 때 사용할 시뮬레이터입니다.
- server port : 8082
- actuator port : 8083

### Getting Started
부트 서버를 아래 명령어 혹은 `intelliJ` 통해 실행해주세요.
```shell
./gradlew :apps:pg-simulator:bootRun
```

API 는 아래와 같이 제공됩니다.
- 결제 요청 API
- 결제 정보 확인 `by transactionKey`
- 결제 정보 목록 조회 `by orderId`

### Behavior

- 모든 API는 `X-USER-ID` 헤더를 요구합니다.
- 결제 요청은 100~500ms 지연 후 처리되며, 요청 단계에서 40% 확률로 `INTERNAL_ERROR`를 반환합니다.
- 결제 요청이 저장되면 응답의 거래 상태는 먼저 `PENDING`입니다.
- 저장 이후 비동기 이벤트가 1~5초 뒤 거래를 처리합니다. 처리 결과는 한도 초과 실패 20%, 잘못된 카드 실패 10%, 승인 성공 70% 확률입니다.
- 처리 결과는 요청 body의 `callbackUrl`로 비동기 통지합니다. 현재 callback URL은 `http://localhost:8080`으로 시작해야 합니다.
- `orderId`는 6자리 이상 문자열, `cardNo`는 `xxxx-xxxx-xxxx-xxxx` 형식, `amount`는 양의 정수여야 합니다.

```http request
### 결제 요청
POST {{pg-simulator}}/api/v1/payments
X-USER-ID: 135135
Content-Type: application/json

{
  "orderId": "1351039135",
  "cardType": "SAMSUNG",
  "cardNo": "1234-5678-9814-1451",
  "amount" : 5000,
  "callbackUrl": "http://localhost:8080/api/v1/examples/callback"
}

### 결제 정보 확인
GET {{pg-simulator}}/api/v1/payments/20250816:TR:9577c5
X-USER-ID: 135135

### 주문에 엮인 결제 정보 조회
GET {{pg-simulator}}/api/v1/payments?orderId=1351039135
X-USER-ID: 135135

```
