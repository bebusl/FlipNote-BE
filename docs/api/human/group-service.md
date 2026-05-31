# Group Service API 문서

**Base URL:** `http://gateway:8080` (모든 요청은 API Gateway를 통해 전달)  
**내부 포트:** HTTP 8084 / gRPC 9094  
**인증:** 모든 엔드포인트는 Bearer JWT 필요. Gateway가 토큰을 검증한 후 `X-USER-ID` 헤더로 사용자 ID를 전달.

---

## 목차

- [그룹 관리](#그룹-관리)
- [멤버 관리](#멤버-관리)
- [초대](#초대)
- [가입 신청](#가입-신청)
- [권한 관리](#권한-관리)

---

## 공통 Enum

| Enum | 값 |
|------|-----|
| Category | `IT`, `ENGLISH`, `MATH`, `SCIENCE`, `HISTORY`, `GEOGRAPHY`, `KOREAN` |
| JoinPolicy | `OPEN` (바로 가입), `APPROVAL` (승인 필요) |
| Visibility | `PUBLIC`, `PRIVATE` |
| GroupMemberRole | `OWNER`(4), `HEAD_MANAGER`(3), `MANAGER`(2), `MEMBER`(1) |
| GroupPermission | `MEMBER_MANAGE`, `JOIN_REQUEST_MANAGE`, `INVITE` |
| InviteStatus | `PENDING`, `ACCEPTED`, `REJECTED`, `EXPIRED` |
| JoinStatus | `ACCEPT`, `PENDING`, `REJECT`, `CANCEL` |

---

## 그룹 관리

### 그룹 생성

새 그룹을 만듭니다. 생성한 사용자는 자동으로 OWNER가 됩니다.

**POST** `/v1/groups`  
인증 필요

#### 요청

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| name | string | ✓ | 그룹 이름 (최대 50자) |
| category | enum | ✓ | 카테고리 |
| description | string | ✓ | 그룹 설명 |
| joinPolicy | enum | ✓ | 가입 정책 (`OPEN` 또는 `APPROVAL`) |
| visibility | enum | ✓ | 공개 여부 (`PUBLIC` 또는 `PRIVATE`) |
| maxMember | number | ✓ | 최대 인원 (1~100) |
| imageRefId | number | - | 이미지 서비스 이미지 ID |

```json
{
  "name": "알고리즘 스터디",
  "category": "IT",
  "description": "매주 알고리즘 문제를 풀어봐요",
  "joinPolicy": "APPROVAL",
  "visibility": "PUBLIC",
  "maxMember": 20,
  "imageRefId": 1
}
```

#### 응답 (200)

```json
{
  "groupId": 42
}
```

#### 에러

| 코드 | 설명 |
|------|------|
| 400 | 이름이 비어있거나 50자 초과, maxMember 범위 오류 등 입력값 오류 |

---

### 그룹 수정

그룹 정보를 수정합니다. OWNER 또는 충분한 권한이 필요합니다.

**PUT** `/v1/groups/{groupId}`  
인증 필요

#### 경로 파라미터

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| groupId | number | 그룹 ID |

#### 요청 (그룹 생성과 동일한 필드)

#### 응답 (200)

```json
{
  "groupId": 42,
  "name": "알고리즘 스터디 v2",
  "category": "IT",
  "description": "수정된 설명",
  "joinPolicy": "OPEN",
  "visibility": "PUBLIC",
  "maxMember": 30,
  "imageRefId": 2,
  "createdAt": "2025-01-01T00:00:00",
  "modifiedAt": "2025-06-01T12:00:00"
}
```

#### 에러

| 코드 | 설명 |
|------|------|
| 403 | 수정 권한 없음 |
| 404 | 그룹 없음 |

---

### 그룹 상세 조회

그룹 상세 정보를 조회합니다. PRIVATE 그룹은 멤버만 조회 가능합니다.

**GET** `/v1/groups/{groupId}`  
인증 필요

#### 응답 (200)

```json
{
  "groupId": 42,
  "name": "알고리즘 스터디",
  "category": "IT",
  "description": "매주 알고리즘 문제를 풀어봐요",
  "joinPolicy": "APPROVAL",
  "visibility": "PUBLIC",
  "maxMember": 20,
  "imageRefId": 1,
  "imageUrl": "https://flipnote-bucket.s3.ap-northeast-2.amazonaws.com/image/1.png",
  "createdAt": "2025-01-01T00:00:00",
  "modifiedAt": "2025-01-01T00:00:00"
}
```

#### 에러

| 코드 | 설명 |
|------|------|
| 403 | 비공개 그룹 (GROUP_004) |
| 404 | 그룹 없음 |

---

### 그룹 삭제

그룹을 삭제합니다. OWNER만 삭제할 수 있습니다.

**DELETE** `/v1/groups/{groupId}`  
인증 필요

#### 응답 (204)

응답 바디 없음

#### 에러

| 코드 | 설명 |
|------|------|
| 403 | 오너가 아님 (PERM_006) |
| 404 | 그룹 없음 |

---

### 전체 그룹 목록 조회 (커서 페이지네이션)

공개 그룹 목록을 조회합니다.

**GET** `/v1/groups`  
인증 필요

#### 쿼리 파라미터

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| cursor | string | - | 이전 응답의 `nextCursor` 값 |
| size | number | 10 | 페이지 크기 (1~30) |
| category | enum | - | 카테고리 필터 |
| groupName | string | - | 그룹 이름 검색 (부분 일치) |

#### 응답 (200)

```json
{
  "content": [
    {
      "groupId": 42,
      "name": "알고리즘 스터디",
      "description": "매주 알고리즘 문제를 풀어봐요",
      "category": "IT",
      "imageRefId": 1,
      "imageUrl": "https://..."
    }
  ],
  "hasNext": true,
  "nextCursor": "41",
  "size": 10
}
```

> **참고:** 다음 페이지를 요청할 때 `nextCursor` 값을 `cursor` 파라미터에 넣으세요.

---

### 내가 가입한 그룹 목록

현재 사용자가 가입한 그룹을 조회합니다.

**GET** `/v1/groups/me`  
인증 필요

쿼리 파라미터와 응답 형식은 [전체 그룹 목록 조회](#전체-그룹-목록-조회-커서-페이지네이션)와 동일합니다.

---

### 내가 만든 그룹 목록

현재 사용자가 생성한 그룹을 조회합니다.

**GET** `/v1/groups/created`  
인증 필요

쿼리 파라미터와 응답 형식은 [전체 그룹 목록 조회](#전체-그룹-목록-조회-커서-페이지네이션)와 동일합니다.

---

### 그룹 오너 여부 확인

현재 사용자가 해당 그룹의 OWNER인지 확인합니다.

**GET** `/v1/groups/{groupId}/managers`  
인증 필요

#### 응답 (200)

```json
{
  "isOwner": true
}
```

---

## 멤버 관리

### 그룹 멤버 전체 조회

그룹에 속한 모든 멤버 목록을 조회합니다.

**GET** `/v1/groups/{groupId}/members`  
인증 필요

#### 응답 (200)

```json
{
  "memberInfoList": [
    {
      "memberId": 1,
      "userId": 100,
      "role": "OWNER",
      "nickname": "홍길동",
      "profileImage": "https://..."
    },
    {
      "memberId": 2,
      "userId": 101,
      "role": "MEMBER",
      "nickname": "김철수",
      "profileImage": null
    }
  ]
}
```

---

### 멤버 강퇴

그룹에서 특정 멤버를 강퇴합니다. MEMBER_MANAGE 권한 또는 대상보다 높은 역할이 필요합니다.

**DELETE** `/v1/groups/{groupId}/members/{memberId}`  
인증 필요

#### 경로 파라미터

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| groupId | number | 그룹 ID |
| memberId | number | 멤버십 ID (userId가 아님) |

#### 응답 (204)

응답 바디 없음

#### 에러

| 코드 | 설명 |
|------|------|
| 403 | 권한 없음, 또는 대상보다 역할이 낮음 |
| 404 | 멤버 없음 |

---

### 멤버 역할 변경

멤버의 역할을 변경합니다. 요청자의 역할이 대상보다 높아야 합니다.

**PUT** `/v1/groups/{groupId}/members/{memberId}`  
인증 필요

#### 요청

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| role | enum | ✓ | 변경할 역할 |

```json
{
  "role": "MANAGER"
}
```

#### 응답 (200)

```json
{
  "memberId": 2,
  "role": "MANAGER"
}
```

#### 에러

| 코드 | 설명 |
|------|------|
| 403 | 권한 없음 또는 역할 부족 |
| 404 | 멤버 없음 |

---

## 초대

### 그룹 초대 발송

이메일로 사용자를 그룹에 초대합니다. INVITE 권한이 필요합니다.

**POST** `/v1/groups/{groupId}/invitations`  
인증 필요

#### 요청

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | string | ✓ | 초대할 사용자 이메일 |

```json
{
  "email": "user@example.com"
}
```

#### 응답 (201)

```json
{
  "invitationId": 7
}
```

#### 에러

| 코드 | 설명 |
|------|------|
| 400 | 본인 초대 불가 (INVITE_004) |
| 403 | 초대 권한 없음 (INVITE_002) |
| 404 | 그룹 없음 |
| 409 | 이미 초대됨 또는 이미 멤버 |

> **참고:** 초대받은 사용자가 FlipNote에 가입된 경우 앱 내 알림을, 미가입 사용자의 경우 이메일로 초대장을 발송합니다.

---

### 초대 취소

발송한 초대를 취소합니다.

**DELETE** `/v1/groups/{groupId}/invitations/{invitationId}`  
인증 필요

#### 응답 (204)

응답 바디 없음

---

### 초대 응답 (수락/거절)

받은 초대에 수락 또는 거절로 응답합니다.

**PATCH** `/v1/groups/{groupId}/invitations/{invitationId}`  
인증 필요

#### 요청

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| status | enum | ✓ | `ACCEPTED` 또는 `REJECTED` |

```json
{
  "status": "ACCEPTED"
}
```

#### 응답 (200)

응답 바디 없음

#### 에러

| 코드 | 설명 |
|------|------|
| 400 | 그룹 인원 초과 (수락 시) |
| 403 | 수신자가 아님 |
| 404 | 초대 없음 |
| 409 | 이미 멤버 (수락 시) |

---

### 그룹에서 보낸 초대 목록 조회

그룹이 발송한 초대 목록을 조회합니다. INVITE 권한이 필요합니다.

**GET** `/v1/groups/{groupId}/invitations`  
인증 필요

#### 쿼리 파라미터

| 파라미터 | 기본값 | 설명 |
|----------|--------|------|
| page | 1 | 페이지 번호 |
| size | 10 | 페이지 크기 (최대 30) |

#### 응답 (200)

```json
{
  "content": [
    {
      "invitationId": 7,
      "inviterUserId": 100,
      "inviteeUserId": 200,
      "inviteeEmail": "user@example.com",
      "inviteeNickname": "김영희",
      "status": "PENDING",
      "createdAt": "2025-06-01T10:00:00"
    }
  ],
  "page": 1,
  "size": 10,
  "totalElements": 3,
  "totalPages": 1,
  "first": true,
  "last": true,
  "hasNext": false,
  "hasPrevious": false
}
```

---

### 내가 받은 초대 목록

현재 사용자가 받은 초대 목록을 조회합니다.

**GET** `/v1/group-invitations`  
인증 필요

#### 쿼리 파라미터

| 파라미터 | 기본값 | 설명 |
|----------|--------|------|
| page | 1 | 페이지 번호 |
| size | 10 | 페이지 크기 (최대 30) |

#### 응답 (200)

```json
{
  "content": [
    {
      "invitationId": 7,
      "groupId": 42,
      "status": "PENDING",
      "createdAt": "2025-06-01T10:00:00"
    }
  ],
  "page": 1,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "hasNext": false,
  "hasPrevious": false
}
```

---

## 가입 신청

`joinPolicy`가 `APPROVAL`인 그룹에 가입할 때 사용합니다.

### 가입 신청 제출

**POST** `/v1/groups/{groupId}/joins`  
인증 필요

#### 요청

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| joinIntro | string | - | 자기소개 메시지 |

```json
{
  "joinIntro": "열심히 참여하겠습니다!"
}
```

#### 응답 (201)

```json
{
  "groupJoinId": 15,
  "status": "PENDING"
}
```

#### 에러

| 코드 | 설명 |
|------|------|
| 400 | 가입 불가 상태 (OPEN 그룹이거나 기타) |
| 403 | 비공개 그룹 |
| 404 | 그룹 없음 |
| 409 | 이미 신청 중이거나 이미 멤버 |

---

### 가입 신청 목록 조회 (관리자용)

그룹에 들어온 가입 신청 목록을 조회합니다. `JOIN_REQUEST_MANAGE` 권한이 필요합니다.

**GET** `/v1/groups/{groupId}/joins`  
인증 필요

#### 응답 (200)

```json
{
  "joinList": [
    {
      "groupJoinId": 15,
      "userId": 105,
      "nickname": "박민수",
      "joinIntro": "열심히 참여하겠습니다!",
      "status": "PENDING"
    }
  ]
}
```

---

### 가입 신청 수락/거절

가입 신청에 응답합니다. `JOIN_REQUEST_MANAGE` 권한이 필요합니다.

**PATCH** `/v1/groups/{groupId}/joins/{joinId}`  
인증 필요

#### 요청

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| status | string | ✓ | `ACCEPT` 또는 `REJECT` |

```json
{
  "status": "ACCEPT"
}
```

#### 응답 (200)

```json
{
  "joinId": 15,
  "status": "ACCEPT"
}
```

#### 에러

| 코드 | 설명 |
|------|------|
| 400 | 그룹 인원 초과 (수락 시) |
| 403 | 권한 없음 |
| 404 | 신청 없음 |
| 409 | 이미 수락됨 |

---

### 가입 신청 취소

본인이 제출한 가입 신청을 취소합니다.

**DELETE** `/v1/groups/{groupId}/joins/{joinId}`  
인증 필요

#### 응답 (200)

응답 바디 없음

---

### 내가 신청한 가입 신청 목록

현재 사용자가 제출한 모든 그룹 가입 신청 목록을 조회합니다.

**GET** `/v1/groups/joins/me`  
인증 필요

#### 응답 (200)

```json
{
  "joinList": [
    {
      "groupJoinId": 15,
      "joinIntro": "열심히 참여하겠습니다!",
      "status": "PENDING",
      "groupId": 42,
      "groupName": "알고리즘 스터디"
    }
  ]
}
```

---

## 권한 관리

그룹 내 역할별 권한을 관리합니다. OWNER만 권한을 추가하거나 제거할 수 있습니다.

**사용 가능한 권한:**
- `MEMBER_MANAGE` — 멤버 강퇴 가능
- `JOIN_REQUEST_MANAGE` — 가입 신청 수락/거절 가능
- `INVITE` — 초대 발송 가능

> **참고:** OWNER는 항상 모든 권한을 가집니다. 하위 역할(HEAD_MANAGER, MANAGER, MEMBER)에 권한을 부여/회수할 수 있습니다.

---

### 권한 부여

특정 역할에 권한을 부여합니다.

**POST** `/v1/groups/{groupId}/permissions`  
인증 필요 (OWNER만 가능)

#### 요청

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| changeRole | enum | ✓ | 권한을 부여할 역할 |
| permission | enum | ✓ | 부여할 권한 |

```json
{
  "changeRole": "MANAGER",
  "permission": "INVITE"
}
```

#### 응답 (200)

```json
{
  "role": "MANAGER",
  "permissions": ["JOIN_REQUEST_MANAGE", "INVITE"]
}
```

#### 에러

| 코드 | 설명 |
|------|------|
| 403 | OWNER가 아님 |
| 404 | 그룹 없음 |
| 409 | 이미 존재하는 권한 |

---

### 권한 회수

특정 역할에서 권한을 제거합니다.

**DELETE** `/v1/groups/{groupId}/permissions`  
인증 필요 (OWNER만 가능)

#### 요청 (권한 부여와 동일)

#### 응답 (200)

```json
{
  "role": "MANAGER",
  "permissions": ["JOIN_REQUEST_MANAGE"]
}
```

---

### 내 권한 확인

현재 사용자의 역할과 권한을 확인합니다.

**GET** `/v1/groups/{groupId}/permissions`  
인증 필요

#### 응답 (200)

```json
{
  "role": "MANAGER",
  "permissions": ["JOIN_REQUEST_MANAGE", "INVITE"]
}
```

#### 에러

| 코드 | 설명 |
|------|------|
| 404 | 해당 그룹의 멤버가 아님 (MEMBER_003) |
