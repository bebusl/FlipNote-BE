# User Service API 문서

FlipNote User Service는 회원가입, 로그인, 소셜 로그인(Google OAuth2), 프로필 관리, JWT 토큰 관리를 담당합니다.

**Base URL:** `https://api3.flipnote.site` (Gateway 경유)  
**내부 포트:** `8081` (HTTP), `9091` (gRPC)

> **인증 방식:** 모든 인증 필요 엔드포인트는 `accessToken` HttpOnly 쿠키를 사용합니다. Bearer 헤더가 아닌 쿠키 기반입니다.

---

## 목차

1. [이메일 인증](#이메일-인증)
2. [회원가입](#회원가입)
3. [로그인 / 로그아웃](#로그인--로그아웃)
4. [토큰 관리](#토큰-관리)
5. [비밀번호 관리](#비밀번호-관리)
6. [소셜 로그인 (OAuth2)](#소셜-로그인-oauth2)
7. [내 정보 조회/수정](#내-정보-조회수정)
8. [다른 사용자 조회](#다른-사용자-조회)
9. [회원 탈퇴](#회원-탈퇴)

---

## 이메일 인증

회원가입 전에 반드시 이메일 인증을 완료해야 합니다.

### 인증 코드 발송

이메일로 6자리 인증 코드를 발송합니다. 코드는 5분간 유효하며 중복 발송은 불가합니다.

**POST** `/v1/auth/email-verification/request`  
인증 불필요

#### 요청

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | string | ✓ | 인증할 이메일 주소 |

```json
{
  "email": "user@example.com"
}
```

#### 응답 (200)

```json
{
  "status": 200,
  "data": null
}
```

#### 에러

| 상태 | 코드 | 설명 |
|------|------|------|
| 409 | ALREADY_ISSUED_VERIFICATION_CODE | 이미 발송된 코드가 있습니다 (5분 대기) |

---

### 인증 코드 확인

받은 6자리 코드로 이메일을 인증합니다. 인증 완료 상태는 10분간 유지됩니다.

**POST** `/v1/auth/email-verification`  
인증 불필요

#### 요청

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | string | ✓ | 인증할 이메일 주소 |
| code | string | ✓ | 6자리 인증 코드 |

```json
{
  "email": "user@example.com",
  "code": "483920"
}
```

#### 응답 (200)

```json
{
  "status": 200,
  "data": null
}
```

#### 에러

| 상태 | 코드 | 설명 |
|------|------|------|
| 400 | NOT_ISSUED_VERIFICATION_CODE | 발송된 코드가 없습니다 |
| 400 | INVALID_VERIFICATION_CODE | 코드가 일치하지 않습니다 |

---

## 회원가입

이메일 인증 완료 후 회원가입을 진행합니다. (인증 완료 후 10분 이내)

**POST** `/v1/auth/register`  
인증 불필요

#### 요청

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | string | ✓ | 이메일 주소 (인증 완료된 이메일) |
| password | string | ✓ | 비밀번호 (8~20자) |
| name | string | ✓ | 실명 |
| nickname | string | ✓ | 닉네임 (2~50자) |
| phone | string | ✓ | 전화번호 (01x로 시작) |
| smsAgree | boolean | ✓ | SMS 마케팅 수신 동의 여부 |

```json
{
  "email": "user@example.com",
  "password": "mypassword123",
  "name": "홍길동",
  "nickname": "길동이",
  "phone": "010-1234-5678",
  "smsAgree": false
}
```

#### 응답 (200)

```json
{
  "status": 200,
  "data": {
    "userId": 42
  }
}
```

#### 에러

| 상태 | 코드 | 설명 |
|------|------|------|
| 400 | — | 유효성 검사 실패 (필드별 에러 목록 반환) |
| 409 | UNVERIFIED_EMAIL | 이메일 인증이 완료되지 않았습니다 |
| 409 | EMAIL_ALREADY_EXISTS | 이미 사용 중인 이메일입니다 |

---

## 로그인 / 로그아웃

### 로그인

이메일과 비밀번호로 로그인합니다. 성공 시 `accessToken`(15분), `refreshToken`(7일)이 HttpOnly 쿠키로 설정됩니다.

**POST** `/v1/auth/login`  
인증 불필요

#### 요청

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | string | ✓ | 이메일 주소 |
| password | string | ✓ | 비밀번호 |

```json
{
  "email": "user@example.com",
  "password": "mypassword123"
}
```

#### 응답 (200)

응답 바디 `data`는 null이며, 쿠키가 자동 설정됩니다.

```
Set-Cookie: accessToken=eyJhbGci...; HttpOnly; Secure; SameSite=Lax; Max-Age=900
Set-Cookie: refreshToken=eyJhbGci...; HttpOnly; Secure; SameSite=Lax; Max-Age=604800
```

#### 에러

| 상태 | 코드 | 설명 |
|------|------|------|
| 401 | INVALID_CREDENTIALS | 비밀번호가 틀렸습니다 |
| 404 | USER_NOT_FOUND | 등록되지 않은 이메일이거나 탈퇴한 계정입니다 |

---

### 로그아웃

현재 세션을 종료하고 쿠키를 삭제합니다.

**POST** `/v1/auth/logout`  
인증 필요 (`accessToken` 쿠키)

#### 요청

별도 요청 바디 없음. `refreshToken` 쿠키가 자동 전송됩니다.

#### 응답 (200)

`accessToken`, `refreshToken` 쿠키가 삭제됩니다.

```json
{
  "status": 200,
  "data": null
}
```

---

## 토큰 관리

### 토큰 갱신

`refreshToken`으로 새로운 토큰 쌍을 발급받습니다. 기존 refresh 토큰은 폐기됩니다.

**POST** `/v1/auth/token/refresh`  
인증 불필요 (refreshToken 쿠키 필요)

#### 요청

별도 요청 바디 없음. `refreshToken` 쿠키가 자동 전송됩니다.

#### 응답 (200)

새로운 `accessToken`, `refreshToken` 쿠키가 설정됩니다.

#### 에러

| 상태 | 코드 | 설명 |
|------|------|------|
| 400 | — | refreshToken 쿠키가 없습니다 |
| 401 | INVALID_TOKEN | 토큰이 유효하지 않거나 만료되었습니다 |
| 401 | BLACKLISTED_TOKEN | 이미 사용된 토큰입니다 |
| 401 | INVALIDATED_SESSION | 세션이 무효화되었습니다 (비밀번호 변경 또는 회원 탈퇴 이후 발급된 토큰) |

---

## 비밀번호 관리

### 비밀번호 변경

현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다. 변경 후 모든 기기의 세션이 만료됩니다.

**PATCH** `/v1/auth/password`  
인증 필요

#### 요청

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| currentPassword | string | ✓ | 현재 비밀번호 |
| newPassword | string | ✓ | 새 비밀번호 (8~20자) |

```json
{
  "currentPassword": "oldpass123",
  "newPassword": "newpass456"
}
```

#### 응답 (200)

`accessToken`, `refreshToken` 쿠키가 삭제됩니다 (재로그인 필요).

#### 에러

| 상태 | 코드 | 설명 |
|------|------|------|
| 401 | PASSWORD_MISMATCH | 현재 비밀번호가 틀렸습니다 |

---

### 비밀번호 재설정 메일 발송

비밀번호를 잊었을 때 재설정 링크를 이메일로 받습니다.

**POST** `/v1/auth/password-reset/request`  
인증 불필요

#### 요청

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | string | ✓ | 가입된 이메일 주소 |

```json
{
  "email": "user@example.com"
}
```

#### 응답 (200)

이메일이 미등록이어도 200을 반환합니다 (이메일 열거 방지).

#### 에러

| 상태 | 코드 | 설명 |
|------|------|------|
| 409 | ALREADY_SENT_PASSWORD_RESET_LINK | 이미 발송된 링크가 있습니다 (30분 대기) |

---

### 비밀번호 재설정

이메일의 링크에 포함된 토큰으로 비밀번호를 변경합니다. 토큰은 30분 후 만료됩니다.

**POST** `/v1/auth/password-reset`  
인증 불필요

#### 요청

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| token | string | ✓ | 재설정 이메일에 포함된 UUID 토큰 |
| password | string | ✓ | 새 비밀번호 (8~20자) |

```json
{
  "token": "3f4a2b1c-...",
  "password": "newpassword123"
}
```

#### 응답 (200)

모든 세션이 만료됩니다 (재로그인 필요).

#### 에러

| 상태 | 코드 | 설명 |
|------|------|------|
| 400 | INVALID_PASSWORD_RESET_TOKEN | 토큰이 만료되었거나 존재하지 않습니다 |

---

## 소셜 로그인 (OAuth2)

Google OAuth2 PKCE 방식을 지원합니다. 서버가 PKCE를 처리하므로 클라이언트는 단순히 URL을 열면 됩니다.

### 소셜 로그인 시작

**GET** `/oauth2/authorization/{provider}`  
인증 불필요 (소셜 로그인) / 인증 필요 (계정 연결 시 `X-User-Id` 필요)

| 파라미터 | 설명 |
|---------|------|
| provider | OAuth 제공자 (`google`) |

- **소셜 로그인:** 그냥 이 URL을 브라우저에서 열면 됩니다.
- **소셜 계정 연결:** `X-User-Id` 헤더가 포함되어 있으면 기존 계정에 소셜 계정을 연결하는 흐름으로 처리됩니다.

브라우저가 Google 로그인 페이지로 리다이렉트됩니다.

---

### 소셜 로그인 콜백 결과

Google 인증 완료 후 클라이언트 앱의 다음 경로로 리다이렉트됩니다.

| 상황 | 리다이렉트 경로 |
|------|----------------|
| 소셜 로그인 성공 | `/social-login/success` (토큰 쿠키 설정됨) |
| 소셜 로그인 실패 | `/social-login/failure` |
| 계정 연결 성공 | `/social-link/success` |
| 계정 연결 실패 | `/social-link/failure` |
| 이미 연결된 소셜 계정 | `/social-link/conflict` |

---

### 연결된 소셜 계정 목록 조회

**GET** `/v1/auth/social-links`  
인증 필요

#### 응답 (200)

```json
{
  "status": 200,
  "data": {
    "socialLinks": [
      {
        "socialLinkId": 1,
        "provider": "google",
        "linkedAt": "2025-10-01T12:34:56"
      }
    ]
  }
}
```

---

### 소셜 계정 연결 해제

**DELETE** `/v1/auth/social-links/{socialLinkId}`  
인증 필요

| 파라미터 | 설명 |
|---------|------|
| socialLinkId | 해제할 소셜 계정 연결 ID |

#### 응답 (200)

```json
{
  "status": 200,
  "data": null
}
```

#### 에러

| 상태 | 설명 |
|------|------|
| 403 | 본인 소유의 연결이 아닙니다 |
| 404 | 연결이 존재하지 않습니다 |

---

## 내 정보 조회/수정

### 내 프로필 조회

**GET** `/v1/users/me`  
인증 필요

#### 응답 (200)

```json
{
  "status": 200,
  "data": {
    "userId": 42,
    "email": "user@example.com",
    "nickname": "길동이",
    "name": "홍길동",
    "phone": "010-1234-5678",
    "smsAgree": false,
    "profileImageUrl": "https://s3.amazonaws.com/...",
    "imageRefId": 7,
    "createdAt": "2025-09-15T10:00:00",
    "modifiedAt": "2025-10-01T12:34:56"
  }
}
```

---

### 프로필 수정

닉네임, 전화번호, SMS 수신 동의, 프로필 이미지를 변경합니다.

**PUT** `/v1/users`  
인증 필요

#### 요청

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| nickname | string | — | 닉네임 (2~50자) |
| phone | string | — | 전화번호 |
| smsAgree | boolean | — | SMS 수신 동의 |
| imageRefId | number | — | Image 서비스에서 발급받은 이미지 참조 ID |

```json
{
  "nickname": "새닉네임",
  "phone": "010-9876-5432",
  "smsAgree": true,
  "imageRefId": 15
}
```

#### 응답 (200)

```json
{
  "status": 200,
  "data": {
    "userId": 42,
    "nickname": "새닉네임",
    "phone": "010-9876-5432",
    "smsAgree": true,
    "profileImageUrl": "https://s3.amazonaws.com/new-image.jpg",
    "imageRefId": 15
  }
}
```

#### 에러

| 상태 | 코드 | 설명 |
|------|------|------|
| 503 | IMAGE_SERVICE_ERROR | Image 서비스 통신 오류 |

---

## 다른 사용자 조회

공개 프로필(닉네임, 프로필 이미지)만 조회합니다.

**GET** `/v1/users/{userId}`  
인증 필요

| 파라미터 | 설명 |
|---------|------|
| userId | 조회할 사용자 ID |

#### 응답 (200)

```json
{
  "status": 200,
  "data": {
    "userId": 10,
    "nickname": "다른유저",
    "profileImageUrl": "https://s3.amazonaws.com/...",
    "imageRefId": 3
  }
}
```

#### 에러

| 상태 | 코드 | 설명 |
|------|------|------|
| 404 | USER_NOT_FOUND | 사용자가 없거나 탈퇴한 계정입니다 |

---

## 회원 탈퇴

계정을 비활성화합니다 (소프트 삭제). 모든 세션이 즉시 만료됩니다.

**DELETE** `/v1/users`  
인증 필요

#### 응답 (200)

`accessToken`, `refreshToken` 쿠키가 삭제됩니다.

```json
{
  "status": 200,
  "data": null
}
```

---

## 공통 에러 응답 형식

```json
{
  "status": 401,
  "code": "INVALID_TOKEN",
  "message": "토큰이 유효하지 않습니다",
  "data": null
}
```

유효성 검사 실패 시 (400):

```json
{
  "status": 400,
  "data": [
    { "field": "password", "message": "8자 이상 20자 이하여야 합니다" }
  ]
}
```
