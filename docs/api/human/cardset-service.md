# Cardset 서비스 API

카드셋(플래시카드 묶음)과 카드를 관리하는 서비스입니다. 카드셋은 반드시 특정 그룹에 속하며, 실시간 협업 편집(Yjs + Socket.IO)을 지원합니다.

**Base URL:** `http://gateway:8080` (API Gateway 경유)  
**인증:** 모든 엔드포인트에 Bearer JWT 필요. Gateway가 토큰을 검증하고 `X-USER-ID` 헤더로 변환해 전달합니다.

---

## 목차

- [카드셋 생성](#카드셋-생성)
- [카드셋 목록 조회](#카드셋-목록-조회)
- [카드셋 단건 조회](#카드셋-단건-조회)
- [카드셋 수정](#카드셋-수정)
- [카드셋 삭제](#카드셋-삭제)
- [카드 목록 조회](#카드-목록-조회)
- [카드 편집 저장](#카드-편집-저장)
- [그룹의 카드셋 목록 조회](#그룹의-카드셋-목록-조회)
- [실시간 협업 (WebSocket)](#실시간-협업-websocket)

---

## 카드셋 생성

그룹 내에 새 카드셋을 만듭니다. 카드셋을 만든 사용자는 자동으로 매니저가 됩니다.

**POST** `/v1/card-sets`  
인증 필요 · 그룹 멤버만 가능

### 요청

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| name | string | ✓ | 카드셋 이름 |
| groupId | number | ✓ | 소속 그룹 ID |
| visibility | string | ✓ | 공개 범위: `PUBLIC` 또는 `PRIVATE` |
| category | string | ✓ | 카테고리 (예: "언어", "수학") |
| hashtag | string | | 해시태그 문자열 (예: "#영어#단어") |
| imageRefId | number | | 대표 이미지 참조 ID (Image 서비스) |
| managerIds | number[] | | 추가 매니저 사용자 ID 배열 |

```json
{
  "name": "영어 단어장",
  "groupId": 1,
  "visibility": "PRIVATE",
  "category": "언어",
  "hashtag": "#영어#단어",
  "imageRefId": 1001,
  "managerIds": [2, 3]
}
```

### 응답 (201)

```json
{
  "status": 201,
  "code": "CREATED",
  "message": "생성되었습니다.",
  "data": {
    "cardsetId": 42
  }
}
```

### 에러

| 상태 | 설명 |
|------|------|
| 403 | 해당 그룹의 멤버가 아님 |

---

## 카드셋 목록 조회

접근 가능한 카드셋 목록을 페이징으로 조회합니다.

**GET** `/v1/card-sets`  
인증 필요

### 쿼리 파라미터

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| page | number | 1 | 페이지 번호 (1부터 시작) |
| size | number | 10 | 페이지당 항목 수 (최대 30) |
| sortBy | string | — | 정렬 기준: `createdAt`, `name`, `cardCount` |
| order | string | desc | 정렬 방향: `asc` 또는 `desc` |
| keyword | string | — | 카드셋 이름 검색 키워드 |
| category | string | — | 카테고리 필터 |

### 응답 (200)

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "성공",
  "data": {
    "items": [
      {
        "cardSetId": 42,
        "groupId": 1,
        "name": "영어 단어장",
        "category": "언어",
        "hashtag": "#영어#단어",
        "imageUrl": "https://s3.amazonaws.com/.../image.png",
        "imageRefId": 1001,
        "likeCount": 15,
        "bookmarkCount": 7,
        "liked": true,
        "bookmarked": false,
        "managers": [
          {
            "id": 1,
            "email": "user@example.com",
            "nickname": "홍길동",
            "profileImageUrl": "https://s3.amazonaws.com/.../profile.png"
          }
        ]
      }
    ],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

---

## 카드셋 단건 조회

카드셋의 상세 정보를 조회합니다.

**GET** `/v1/card-sets/:cardsetId`  
인증 필요

### 경로 파라미터

| 파라미터 | 설명 |
|---------|------|
| cardsetId | 조회할 카드셋 ID |

### 응답 (200)

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "성공",
  "data": {
    "id": 42,
    "name": "영어 단어장",
    "groupId": 1,
    "visibility": "PRIVATE",
    "category": "언어",
    "hashtag": "#영어#단어",
    "imageRefId": 1001,
    "imageUrl": "https://s3.amazonaws.com/.../image.png",
    "cardCount": 30,
    "likeCount": 15,
    "bookmarkCount": 7,
    "liked": true,
    "bookmarked": false,
    "managers": [
      {
        "id": 1,
        "email": "user@example.com",
        "nickname": "홍길동",
        "profileImageUrl": "https://s3.amazonaws.com/.../profile.png"
      }
    ],
    "createdAt": "2024-01-15T09:00:00.000Z",
    "updatedAt": "2024-01-20T14:30:00.000Z"
  }
}
```

### 에러

| 상태 | 설명 |
|------|------|
| 403 | 해당 카드셋에 접근 권한 없음 (PRIVATE 카드셋인 경우) |

---

## 카드셋 수정

카드셋 정보를 수정합니다. 매니저만 수정 가능합니다.

**PUT** `/v1/card-sets/:cardsetId`  
인증 필요 · 매니저 권한 필요

### 경로 파라미터

| 파라미터 | 설명 |
|---------|------|
| cardsetId | 수정할 카드셋 ID |

### 요청

수정할 필드만 포함하면 됩니다.

| 필드 | 타입 | 설명 |
|------|------|------|
| name | string | 새 카드셋 이름 |
| visibility | string | `PUBLIC` 또는 `PRIVATE` |
| category | string | 카테고리 |
| hashtag | string \| null | 해시태그 (null로 설정 시 삭제) |
| imageRefId | number | 이미지 참조 ID |
| managerIds | number[] | 매니저 목록 (전체 교체) |

```json
{
  "name": "수정된 단어장",
  "visibility": "PUBLIC",
  "category": "수학"
}
```

### 응답 (200)

수정된 카드셋 상세 정보를 반환합니다 (단건 조회와 동일한 구조).

### 에러

| 상태 | 설명 |
|------|------|
| 403 | 매니저 권한 없음 |

---

## 카드셋 삭제

카드셋을 삭제합니다. 매니저만 삭제 가능합니다.

**DELETE** `/v1/card-sets/:cardsetId`  
인증 필요 · 매니저 권한 필요

### 경로 파라미터

| 파라미터 | 설명 |
|---------|------|
| cardsetId | 삭제할 카드셋 ID |

### 응답 (200)

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "삭제되었습니다.",
  "data": null
}
```

### 에러

| 상태 | 설명 |
|------|------|
| 403 | 매니저 권한 없음 |

---

## 카드 목록 조회

카드셋에 속한 카드 목록을 조회합니다. Yjs CRDT 문서에서 읽어온 현재 스냅샷을 반환합니다.

**GET** `/v1/card-sets/:cardsetId/cards`  
인증 필요

### 경로 파라미터

| 파라미터 | 설명 |
|---------|------|
| cardsetId | 카드셋 ID |

### 응답 (200)

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "성공",
  "data": [
    {
      "id": "card-uuid-1",
      "question": "apple",
      "answer": "사과"
    },
    {
      "id": "card-uuid-2",
      "question": "banana",
      "answer": "바나나"
    }
  ]
}
```

> **참고:** 카드 `id`는 Yjs 문서 내부의 문자열 식별자입니다.

---

## 카드 편집 저장

현재 Yjs 문서 상태를 DB에 영구 저장합니다. 매니저만 가능합니다.

**POST** `/v1/card-sets/:cardsetId`  
인증 필요 · 매니저 권한 필요

### 경로 파라미터

| 파라미터 | 설명 |
|---------|------|
| cardsetId | 카드셋 ID |

### 응답 (200)

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "성공",
  "data": null
}
```

### 에러

| 상태 | 설명 |
|------|------|
| 403 | 매니저 권한 없음 |

---

## 그룹의 카드셋 목록 조회

특정 그룹에 속한 카드셋 목록을 페이징으로 조회합니다.

**GET** `/v1/groups/:groupId/card-sets`  
인증 필요 · 그룹 멤버만 가능

### 경로 파라미터

| 파라미터 | 설명 |
|---------|------|
| groupId | 조회할 그룹 ID |

### 쿼리 파라미터

카드셋 목록 조회와 동일한 파라미터를 사용합니다 (`page`, `size`, `sortBy`, `order`, `keyword`, `category`).

### 응답 (200)

카드셋 목록 조회와 동일한 구조입니다.

### 에러

| 상태 | 설명 |
|------|------|
| 403 | 해당 그룹의 멤버가 아님 |

---

## 실시간 협업 (WebSocket)

Socket.IO를 통해 카드셋을 여러 사용자가 동시에 편집할 수 있습니다. Yjs CRDT를 기반으로 충돌 없는 실시간 동기화를 지원합니다.

**WebSocket 경로:** `ws://gateway:8080/v1/card-sets/ws`  
인증 필요 · Socket.IO 핸드쉐이크 시 JWT 전달

> **매니저 권한 필요:** `join-cardset` 이벤트 시 편집 권한(매니저) 여부를 확인합니다.

### 연결

```javascript
import { io } from 'socket.io-client';

const socket = io('http://gateway:8080', {
  path: '/v1/card-sets/ws',
  auth: { token: 'Bearer eyJhbGci...' },
});
```

---

### 이벤트: join-cardset (클라이언트 → 서버)

카드셋 편집 방에 입장합니다. 성공 시 서버가 현재 Yjs 문서 전체 상태를 `sync` 이벤트로 응답합니다.

```javascript
socket.emit('join-cardset', { cardsetId: '42' });
```

| 필드 | 타입 | 설명 |
|------|------|------|
| cardsetId | string | 입장할 카드셋 ID |

---

### 이벤트: leave-cardset (클라이언트 → 서버)

카드셋 편집 방에서 퇴장합니다. 방의 마지막 사용자가 퇴장하면 5초 후 Yjs 문서가 DB에 자동 저장됩니다.

```javascript
socket.emit('leave-cardset', { cardsetId: '42' });
```

---

### 이벤트: update (클라이언트 → 서버)

Yjs 증분 업데이트를 서버에 전송합니다. 서버는 업데이트를 병합하고 방 전체에 `sync`를 브로드캐스트합니다.

```javascript
// Yjs 문서 변경 시
doc.on('update', (update) => {
  socket.emit('update', {
    cardsetId: '42',
    update: Array.from(update),   // Uint8Array → number[]
  });
});
```

| 필드 | 타입 | 설명 |
|------|------|------|
| cardsetId | string | 카드셋 ID |
| update | number[] | Yjs 증분 업데이트 (바이트 배열) |

---

### 이벤트: awareness (클라이언트 → 서버)

커서 위치, 선택 영역 등 사용자 존재감(awareness) 상태를 다른 클라이언트에게 전파합니다.

```javascript
socket.emit('awareness', {
  cardsetId: '42',
  awareness: Array.from(awarenessUpdate),
});
```

---

### 이벤트: sync (서버 → 클라이언트)

Yjs 문서 상태를 전달합니다. `join-cardset` 직후에는 전체 상태를, 이후에는 증분 업데이트를 수신합니다.

```javascript
socket.on('sync', ({ cardsetId, update }) => {
  const updateBuffer = new Uint8Array(update);
  Y.applyUpdate(doc, updateBuffer);
});
```

| 필드 | 타입 | 설명 |
|------|------|------|
| cardsetId | string | 카드셋 ID |
| update | number[] | Yjs 문서 상태 (바이트 배열) |

---

### 이벤트: error (서버 → 클라이언트)

오류 발생 시 서버가 전송합니다.

```javascript
socket.on('error', ({ message }) => {
  console.error('WS error:', message);
});
```

| 메시지 | 설명 |
|--------|------|
| 카드셋 편집 권한이 없습니다. | join-cardset 시 매니저 권한 없음 |
| Update data is required | update 이벤트에 update 필드 누락 |
| Sync failed | 서버 내부 동기화 오류 |

---

### 실시간 협업 플로우

```
클라이언트                          서버
    |                                  |
    |-- join-cardset {cardsetId} ----->|
    |                                  | Redis에서 Yjs 문서 로드
    |                                  | (없으면 DB에서 로드 또는 신규 생성)
    |<-- sync {update: 전체상태} ------|
    |                                  |
    | (편집 발생)                       |
    |-- update {cardsetId, update} --->|
    |                                  | 업데이트 병합 후 방 전체 브로드캐스트
    |<-- sync {update: 머지 결과} ------|
    |                                  |
    |-- leave-cardset {cardsetId} ---->|
    |                                  | 마지막 사용자면 5초 후 DB 저장
```
