# Reaction 서비스 API

좋아요(Like)와 북마크(Bookmark) 기능을 제공합니다. 현재 카드셋(`card_set`)을 대상으로 지원합니다.

**Base URL:** `http://gateway:8080`  
**인증:** 모든 엔드포인트는 Bearer JWT 토큰이 필요합니다. Gateway에서 토큰 검증 후 `X-User-Id` 헤더로 변환해 전달합니다.

---

## 목차

- [좋아요 추가](#좋아요-추가)
- [좋아요 취소](#좋아요-취소)
- [좋아요 목록 조회](#좋아요-목록-조회)
- [북마크 추가](#북마크-추가)
- [북마크 취소](#북마크-취소)
- [북마크 목록 조회](#북마크-목록-조회)

---

## 좋아요 추가

특정 카드셋에 좋아요를 추가합니다.

**POST** `/v1/likes/{targetType}/{targetId}`  
인증 필요

### 경로 파라미터

| 파라미터 | 타입 | 설명 |
|---|---|---|
| targetType | string | 대상 타입. 현재 `card_set`만 지원 |
| targetId | number | 좋아요할 대상의 ID |

### 응답 (200)

```json
{
  "id": 42
}
```

### 에러

| 코드 | 설명 |
|---|---|
| 400 LIKE_001 | 유효하지 않은 대상 타입 (`card_set` 이외의 값) |
| 403 COMMON_003 | 접근 권한이 없는 대상 |
| 409 LIKE_002 | 이미 좋아요한 대상 |
| 502 COMMON_004 | 카드셋 서비스 조회 실패 |

---

## 좋아요 취소

이전에 추가한 좋아요를 취소합니다.

**DELETE** `/v1/likes/{targetType}/{targetId}`  
인증 필요

### 경로 파라미터

| 파라미터 | 타입 | 설명 |
|---|---|---|
| targetType | string | 대상 타입. 현재 `card_set`만 지원 |
| targetId | number | 좋아요를 취소할 대상의 ID |

### 응답 (200)

응답 바디 없음

### 에러

| 코드 | 설명 |
|---|---|
| 400 LIKE_001 | 유효하지 않은 대상 타입 |
| 404 LIKE_003 | 해당 좋아요 기록을 찾을 수 없음 |

---

## 좋아요 목록 조회

내가 좋아요한 항목 목록을 페이지네이션으로 조회합니다.

**GET** `/v1/likes/{targetType}`  
인증 필요

### 경로 파라미터

| 파라미터 | 타입 | 설명 |
|---|---|---|
| targetType | string | 대상 타입. 현재 `card_set`만 지원 |

### 쿼리 파라미터

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| page | number | 1 | 페이지 번호 (1부터 시작) |
| size | number | 10 | 페이지당 항목 수 (최대 30) |
| sortBy | string | — | 정렬 기준 필드명 |
| order | string | desc | 정렬 방향 (`asc` 또는 `desc`) |

### 응답 (200)

```json
{
  "content": [
    {
      "targetType": "CARD_SET",
      "targetId": 101,
      "likedAt": "2025-05-20T10:30:00",
      "cardSet": {
        "id": 101,
        "name": "영어 단어장",
        "groupId": 5,
        "visibility": "PUBLIC",
        "category": "LANGUAGE",
        "hashtag": "#영어 #토익",
        "imageRefId": 12,
        "cardCount": 50
      }
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "hasNext": false,
  "hasPrevious": false
}
```

> `page` 필드는 응답에서 0-based로 반환됩니다 (요청은 1부터 시작).

### 에러

| 코드 | 설명 |
|---|---|
| 400 LIKE_001 | 유효하지 않은 대상 타입 |
| 400 COMMON_002 | 쿼리 파라미터 유효성 오류 (page/size 범위 초과 등) |

---

## 북마크 추가

특정 카드셋에 북마크를 추가합니다.

**POST** `/v1/bookmarks/{targetType}/{targetId}`  
인증 필요

### 경로 파라미터

| 파라미터 | 타입 | 설명 |
|---|---|---|
| targetType | string | 대상 타입. 현재 `card_set`만 지원 |
| targetId | number | 북마크할 대상의 ID |

### 응답 (200)

```json
{
  "id": 88
}
```

### 에러

| 코드 | 설명 |
|---|---|
| 400 BOOKMARK_001 | 유효하지 않은 대상 타입 |
| 403 COMMON_003 | 접근 권한이 없는 대상 |
| 409 BOOKMARK_002 | 이미 북마크한 대상 |
| 502 COMMON_004 | 카드셋 서비스 조회 실패 |

---

## 북마크 취소

이전에 추가한 북마크를 취소합니다.

**DELETE** `/v1/bookmarks/{targetType}/{targetId}`  
인증 필요

### 경로 파라미터

| 파라미터 | 타입 | 설명 |
|---|---|---|
| targetType | string | 대상 타입. 현재 `card_set`만 지원 |
| targetId | number | 북마크를 취소할 대상의 ID |

### 응답 (200)

응답 바디 없음

### 에러

| 코드 | 설명 |
|---|---|
| 400 BOOKMARK_001 | 유효하지 않은 대상 타입 |
| 404 BOOKMARK_003 | 해당 북마크 기록을 찾을 수 없음 |

---

## 북마크 목록 조회

내가 북마크한 항목 목록을 페이지네이션으로 조회합니다.

**GET** `/v1/bookmarks/{targetType}`  
인증 필요

### 경로 파라미터

| 파라미터 | 타입 | 설명 |
|---|---|---|
| targetType | string | 대상 타입. 현재 `card_set`만 지원 |

### 쿼리 파라미터

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| page | number | 1 | 페이지 번호 (1부터 시작) |
| size | number | 10 | 페이지당 항목 수 (최대 30) |
| sortBy | string | — | 정렬 기준 필드명 |
| order | string | desc | 정렬 방향 (`asc` 또는 `desc`) |

### 응답 (200)

```json
{
  "content": [
    {
      "targetType": "CARD_SET",
      "targetId": 101,
      "bookmarkedAt": "2025-05-20T11:00:00",
      "cardSet": {
        "id": 101,
        "name": "영어 단어장",
        "groupId": 5,
        "visibility": "PUBLIC",
        "category": "LANGUAGE",
        "hashtag": "#영어 #토익",
        "imageRefId": 12,
        "cardCount": 50
      }
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "hasNext": false,
  "hasPrevious": false
}
```

### 에러

| 코드 | 설명 |
|---|---|
| 400 BOOKMARK_001 | 유효하지 않은 대상 타입 |
| 400 COMMON_002 | 쿼리 파라미터 유효성 오류 |
