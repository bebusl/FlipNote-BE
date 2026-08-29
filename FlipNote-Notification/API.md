# FlipNote Notification API

Base URL: `http://localhost:8086`

---

## 공통

### 요청 헤더

모든 API에 아래 헤더가 필요합니다.

| 헤더 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `X-User-Id` | `Long` | Y | 요청 유저 ID |

### 응답 형식

성공/실패 모두 동일한 구조로 래핑됩니다.

```json
{
  "status": 200,
  "code": null,
  "message": null,
  "data": { }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `status` | `int` | HTTP 상태 코드 |
| `code` | `string \| null` | 에러 코드 (성공 시 null) |
| `message` | `string \| null` | 에러 메시지 (성공 시 null) |
| `data` | `any \| null` | 응답 본문 (에러 시 null) |

### 에러 코드

| code | status | 메시지 |
|---|---|---|
| `NOTIFICATION_001` | 500 | FCM 내부 오류가 발생했습니다 |
| `NOTIFICATION_002` | 503 | FCM 서버를 사용할 수 없습니다. |
| `NOTIFICATION_003` | 404 | 알림이 존재하지 않습니다. |
| `NOTIFICATION_004` | 409 | 이미 읽은 알림입니다. |
| `COMMON_001` | 500 | 예기치 않은 오류가 발생했습니다. |
| `COMMON_002` | 400 | 입력값이 올바르지 않습니다. |

---

## 알림 목록 조회

앱 진입 시, 또는 FCM 수신 후 목록을 갱신할 때 호출합니다.
커서 기반 페이징을 사용합니다.

```
GET /v1/notifications
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `cursor` | `string` | N | - | 이전 응답의 `nextCursor` 값. 첫 페이지는 생략 |
| `size` | `int` | N | `10` | 페이지 크기 (1~30) |
| `sortBy` | `string` | N | - | 정렬 기준 필드명. 생략 시 DB 기본 순서 |
| `order` | `string` | N | `desc` | 정렬 방향 (`asc` \| `desc`) |
| `groupId` | `Long` | N | - | 특정 그룹의 알림만 필터링 |
| `read` | `boolean` | N | - | `true`: 읽은 알림만 / `false`: 안 읽은 알림만 / 생략: 전체 |

### 응답

```json
{
  "status": 200,
  "code": null,
  "message": null,
  "data": {
    "content": [
      {
        "notificationId": 42,
        "groupId": 7,
        "message": "스터디 그룹에 초대되셨습니다.",
        "metadata": {},
        "isRead": false,
        "readAt": null,
        "createdAt": "2024-03-15T10:30:00"
      }
    ],
    "hasNext": true,
    "nextCursor": "42",
    "size": 10
  }
}
```

**`data` 필드**

| 필드 | 타입 | 설명 |
|---|---|---|
| `content` | `Notification[]` | 알림 목록 |
| `hasNext` | `boolean` | 다음 페이지 존재 여부 |
| `nextCursor` | `string \| null` | 다음 페이지 요청 시 `cursor`에 사용. 마지막 페이지면 `null` |
| `size` | `int` | 현재 페이지에 담긴 항목 수 |

**`Notification` 객체**

| 필드 | 타입 | 설명 |
|---|---|---|
| `notificationId` | `Long` | 알림 ID |
| `groupId` | `Long \| null` | 관련 그룹 ID |
| `message` | `string` | 표시할 알림 메시지 (이미 완성된 문자열) |
| `metadata` | `object` | 타입별 추가 데이터 (아래 참조) |
| `isRead` | `boolean` | 읽음 여부 |
| `readAt` | `string \| null` | 읽은 시각 (ISO 8601). 안 읽었으면 `null` |
| `createdAt` | `string` | 생성 시각 (ISO 8601) |

**알림 타입별 `metadata`**

| 알림 종류 | `message` 예시 | `metadata` |
|---|---|---|
| 그룹 초대 | `"스터디 그룹에 초대되셨습니다."` | `{}` |
| 그룹 가입 신청 | `"홍길동님이 그룹 가입을 신청했습니다."` | `{ "requesterId": 123 }` |

### 페이징 예시

```
# 첫 페이지
GET /v1/notifications?size=10

# 다음 페이지 (이전 응답의 nextCursor 사용)
GET /v1/notifications?size=10&cursor=42
```

---

## FCM 토큰 등록

앱 실행 시 Firebase에서 발급받은 FCM 토큰을 서버에 등록합니다.
같은 토큰을 중복 등록해도 안전합니다.

```
POST /v1/notifications/token
```

### Request Body

```json
{
  "token": "fcm-device-token-string"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `token` | `string` | Y | FCM 디바이스 토큰 (빈 문자열 불가) |

### 응답

```
HTTP 201 Created
```

```json
{
  "status": 201,
  "code": null,
  "message": null,
  "data": null
}
```

---

## 전체 알림 읽음 처리

해당 유저의 모든 미읽음 알림을 일괄 읽음 처리합니다.

```
POST /v1/notifications/read-all
```

### 응답

```
HTTP 200 OK
```

```json
{
  "status": 200,
  "code": null,
  "message": null,
  "data": null
}
```

---

## 개별 알림 읽음 처리

특정 알림 하나를 읽음 처리합니다.

```
POST /v1/notifications/{notificationId}/read
```

### Path Parameters

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `notificationId` | `Long` | 읽음 처리할 알림 ID |

### 응답

```
HTTP 200 OK
```

```json
{
  "status": 200,
  "code": null,
  "message": null,
  "data": null
}
```

### 에러 케이스

| 상황 | status | code |
|---|---|---|
| 존재하지 않는 알림 ID, 또는 본인 알림이 아닌 경우 | 404 | `NOTIFICATION_003` |
| 이미 읽은 알림 | 409 | `NOTIFICATION_004` |

---

## FCM 실시간 알림 수신

서버는 알림 생성 시 등록된 FCM 토큰으로 push를 자동 발송합니다.
FCM 메시지에는 **title / body 텍스트만** 포함되며 별도 data payload는 없습니다.

| 상황 | 처리 방법 |
|---|---|
| **포그라운드** | `onMessage` 이벤트 → `GET /v1/notifications` 재호출해서 목록 갱신 |
| **백그라운드** | 시스템 트레이 알림 자동 표시 → 유저 탭 → 앱 오픈 후 `GET /v1/notifications` 호출 |
