# Notification 서비스 API

Firebase FCM 기반 푸시 알림 발송 및 알림 내역 관리 서비스입니다.

**Base URL (Gateway):** `http://localhost:8080`  
**내부 포트:** `8086`

---

## 공통

### 인증

모든 엔드포인트는 Bearer JWT 인증이 필요합니다.  
Gateway가 토큰을 검증한 후 `X-User-Id` 헤더로 변환해 전달하므로, 클라이언트는 `Authorization: Bearer {token}` 헤더만 포함하면 됩니다.

### 응답 형식

```json
{
  "status": 200,
  "code": null,
  "message": null,
  "data": { }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `status` | `int` | HTTP 상태 코드 |
| `code` | `string \| null` | 에러 코드 (성공 시 `null`) |
| `message` | `string \| null` | 에러 메시지 (성공 시 `null`) |
| `data` | `any \| null` | 응답 본문 (에러 시 `null`) |

### 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| `NOTIFICATION_001` | 500 | FCM 내부 오류 |
| `NOTIFICATION_002` | 503 | FCM 서버를 사용할 수 없음 |
| `NOTIFICATION_003` | 404 | 알림이 존재하지 않거나 본인 알림이 아님 |
| `NOTIFICATION_004` | 409 | 이미 읽은 알림 |
| `COMMON_001` | 500 | 예기치 않은 서버 오류 |
| `COMMON_002` | 400 | 입력값이 올바르지 않음 |

---

## 알림 목록 조회

로그인한 사용자의 알림 내역을 커서 기반으로 페이징 조회합니다.  
앱 진입 시 또는 FCM 수신 후 목록을 갱신할 때 호출합니다.

**GET** `/v1/notifications`  
인증 필요

### 쿼리 파라미터

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `cursor` | `string` | N | - | 이전 응답의 `nextCursor` 값. 첫 페이지는 생략 |
| `size` | `int` | N | `10` | 페이지 크기 (1~30) |
| `sortBy` | `string` | N | - | 정렬 기준 필드명. 생략 시 DB 기본 순서 |
| `order` | `string` | N | `desc` | 정렬 방향 (`asc` \| `desc`) |
| `groupId` | `number` | N | - | 특정 그룹의 알림만 필터링 |
| `read` | `boolean` | N | - | `true`: 읽은 알림만 / `false`: 안 읽은 알림만 / 생략: 전체 |

### 응답 (200)

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
      },
      {
        "notificationId": 38,
        "groupId": 5,
        "message": "홍길동님이 그룹 가입을 신청했습니다.",
        "metadata": { "requesterId": 123 },
        "isRead": true,
        "readAt": "2024-03-14T09:00:00",
        "createdAt": "2024-03-14T08:55:00"
      }
    ],
    "hasNext": true,
    "nextCursor": "38",
    "size": 2
  }
}
```

**`data` 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| `content` | `Notification[]` | 알림 목록 |
| `hasNext` | `boolean` | 다음 페이지 존재 여부 |
| `nextCursor` | `string \| null` | 다음 페이지 요청 시 `cursor`에 사용. 마지막 페이지면 `null` |
| `size` | `int` | 현재 페이지에 담긴 항목 수 |

**알림 객체 (`Notification`)**

| 필드 | 타입 | 설명 |
|------|------|------|
| `notificationId` | `number` | 알림 ID |
| `groupId` | `number \| null` | 관련 그룹 ID |
| `message` | `string` | 표시할 알림 메시지 (완성된 문자열) |
| `metadata` | `object` | 알림 유형별 추가 데이터 (아래 참조) |
| `isRead` | `boolean` | 읽음 여부 |
| `readAt` | `string \| null` | 읽은 시각 (ISO 8601). 안 읽었으면 `null` |
| `createdAt` | `string` | 생성 시각 (ISO 8601) |

**알림 유형별 `metadata`**

| 유형 | `message` 예시 | `metadata` |
|------|----------------|------------|
| 그룹 초대 | `"스터디 그룹에 초대되셨습니다."` | `{}` |
| 그룹 가입 신청 | `"홍길동님이 그룹 가입을 신청했습니다."` | `{ "requesterId": 123 }` |

### 페이징 예시

```bash
# 첫 페이지 (읽지 않은 알림만, 5개씩)
GET /v1/notifications?size=5&read=false

# 다음 페이지 (이전 응답의 nextCursor 사용)
GET /v1/notifications?size=5&read=false&cursor=38

# 특정 그룹의 알림만
GET /v1/notifications?groupId=7
```

---

## FCM 토큰 등록

앱 실행 시 Firebase에서 발급받은 FCM 디바이스 토큰을 서버에 등록합니다.  
같은 토큰을 중복 등록해도 안전합니다.

**POST** `/v1/notifications/token`  
인증 필요

### 요청

```json
{
  "token": "fcm-device-token-string"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `token` | `string` | ✓ | FCM 디바이스 토큰 (빈 문자열 불가) |

### 응답 (201)

```json
{
  "status": 201,
  "code": null,
  "message": null,
  "data": null
}
```

### 사용 시점

앱 초기화 시 Firebase SDK에서 `getToken()`으로 받은 토큰을 즉시 등록하세요.  
토큰이 갱신될 때(`onTokenRefresh`)도 재등록이 필요합니다.

---

## 전체 알림 읽음 처리

로그인한 사용자의 모든 미읽음 알림을 일괄 읽음 처리합니다.

**POST** `/v1/notifications/read-all`  
인증 필요

### 응답 (200)

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

**POST** `/v1/notifications/{notificationId}/read`  
인증 필요

### 경로 파라미터

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `notificationId` | `number` | 읽음 처리할 알림 ID |

### 응답 (200)

```json
{
  "status": 200,
  "code": null,
  "message": null,
  "data": null
}
```

### 에러

| 상황 | HTTP | 코드 |
|------|------|------|
| 존재하지 않는 알림 ID 또는 본인 알림이 아님 | 404 | `NOTIFICATION_003` |
| 이미 읽은 알림 | 409 | `NOTIFICATION_004` |

---

## FCM 실시간 푸시 알림

서버는 알림 생성 시 해당 사용자의 등록된 FCM 토큰으로 푸시를 자동 발송합니다.  
FCM 메시지에는 **title / body 텍스트만** 포함되며 별도 data payload는 없습니다.

| 상황 | 처리 방법 |
|------|----------|
| **포그라운드** | `onMessage` 이벤트 수신 → `GET /v1/notifications` 재호출로 목록 갱신 |
| **백그라운드** | 시스템 트레이 알림 자동 표시 → 사용자 탭 → 앱 오픈 후 `GET /v1/notifications` 호출 |
