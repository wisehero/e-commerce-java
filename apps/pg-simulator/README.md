## PG-Simulator (PaymentGateway)

### Description
로컬 개발과 결제 연동 검증을 위해 PaymentGateway 를 시뮬레이션하는 App Module 입니다.
PayPal의 주문·매입·환불과 Eximbay의 거래·매입·취소 개념만 단순화해 흉내 내며, 실제 PG API 규격을 복제하지는 않습니다.
`local` 프로필로 실행 권장하며, 커머스 서비스와의 동시 실행을 위해 서버 포트가 조정되어 있습니다.
- server port : 8082
- actuator port : 8083

### Getting Started
부트 서버를 아래 명령어 혹은 `intelliJ` 통해 실행해주세요.
```shell
./gradlew :apps:pg-simulator:bootRun
```

`commerce-api`는 기본 설정(`commerce.payment.gateway=pg-simulator`)에서 이 앱을 직접 호출합니다.
로컬에서 주문 결제 흐름을 확인하려면 PG 시뮬레이터를 먼저 띄우고, 별도 터미널에서 커머스 API를 실행합니다.

```shell
./gradlew :apps:commerce-api:bootRun
```

커머스 API 쪽 기본 연동 설정은 다음과 같습니다.

- PG base URL: `http://localhost:8082`
- 결제 요청 사용자 헤더: `X-USER-ID: commerce-api`
- 기본 카드: `SAMSUNG` / `1234-5678-9814-1451`
- callback URL: `http://localhost:8080/api/v1/payments/callback`

현재 커머스 API는 callback 대신 `transactionKey` 상태 조회를 사용합니다. PG 시뮬레이터가 callback URL prefix를 `http://localhost:8080`으로 검증하므로 기본 callback URL은 이 제약에 맞춰 둡니다.

결제 상태는 다음과 같이 전이됩니다.

```text
PENDING -> CAPTURED -> PARTIALLY_REFUNDED -> REFUNDED
        -> FAILED
```

`CAPTURED`는 승인과 매입을 한 번에 처리한 최종 결제 성공 상태입니다. `AUTHORIZED` 상태는 별도로 두지 않습니다.

기존 로컬 DB에 `payments` 테이블이 있다면 아래 스키마 변경을 한 번 적용해야 합니다. 새 Docker 볼륨에는 초기화 SQL이 자동 적용됩니다.

```sql
ALTER TABLE commerce_pg.payments
  MODIFY COLUMN status VARCHAR(32) NOT NULL,
  ADD COLUMN refunded_amount BIGINT NOT NULL DEFAULT 0 AFTER amount;

UPDATE commerce_pg.payments
SET status = 'CAPTURED'
WHERE status = 'SUCCESS';
```

API 는 아래와 같이 주어지니, 커머스 서비스와 동시에 실행시킨 후 진행해주시면 됩니다.
- 결제 요청 API
- 결제 정보 확인 `by transactionKey`
- 결제 정보 목록 조회 `by orderId`
- 전체·부분 환불

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

### 부분 또는 전체 환불
POST {{pg-simulator}}/api/v1/payments/20250816:TR:9577c5/refunds
X-USER-ID: 135135
Content-Type: application/json

{
  "amount": 2000
}

```
