# User Service 아키텍처

## 서비스 목적 및 책임

User Service는 FlipNote의 인증(Authentication) 및 사용자 관리(User Management) 전담 서비스다.

**핵심 책임:**
- 이메일 기반 회원가입 (이메일 인증 선행 필수)
- 이메일/비밀번호 로그인, 소셜 로그인(Google OAuth2 PKCE)
- JWT 발급·갱신·검증·폐기 (AccessToken 15분 / RefreshToken 7일)
- 세션 무효화 (비밀번호 변경 및 회원 탈퇴 시 전 세션 즉시 만료)
- 프로필 조회 및 수정 (이미지 변경은 Image 서비스와 gRPC 연동)
- 타 서비스에 사용자 정보 제공 (gRPC UserQueryService)

---

## 디렉토리 구조

```
FlipNote-User/src/main/java/flipnote/user/
├── interfaces/
│   ├── http/                        # HTTP 진입점
│   │   ├── AuthController.java      # /v1/auth/** 엔드포인트
│   │   ├── UserController.java      # /v1/users/** 엔드포인트
│   │   └── OAuthController.java     # /oauth2/authorization|callback/**
│   └── grpc/
│       └── GrpcUserQueryService.java  # gRPC 서비스 구현 (포트 9091)
├── application/                     # 비즈니스 로직
│   ├── AuthService.java
│   ├── UserService.java
│   └── OAuthService.java
├── domain/                          # 핵심 도메인
│   ├── entity/
│   │   ├── User.java
│   │   └── OAuthLink.java
│   └── repository/
│       ├── UserRepository.java
│       └── OAuthLinkRepository.java
└── infrastructure/
    ├── jwt/
    │   ├── JwtProvider.java         # 토큰 생성·검증
    │   └── JwtProperties.java       # secret, TTL 설정
    ├── redis/                       # Redis 기반 상태 저장소
    │   ├── TokenBlacklistRepository.java
    │   ├── EmailVerificationRepository.java
    │   ├── PasswordResetRepository.java
    │   ├── SessionInvalidationRepository.java
    │   └── SocialLinkTokenRepository.java
    ├── oauth/
    │   ├── OAuthApiClient.java      # 외부 OAuth2 통신
    │   ├── OAuthProperties.java
    │   └── PkceUtil.java
    ├── grpc/
    │   └── ImageServiceClient.java  # Image 서비스 gRPC 호출
    └── config/                      # Spring 설정 클래스들
```

---

## 핵심 클래스/파일 목록

| 파일 | 역할 |
|------|------|
| `AuthController` | 로그인, 회원가입, 토큰, 비밀번호, 이메일 인증 REST 엔드포인트 |
| `UserController` | 내 정보 조회/수정, 다른 사용자 공개 프로필 조회 |
| `OAuthController` | OAuth2 인증 시작 및 콜백 처리 |
| `GrpcUserQueryService` | GetUser / GetUsers / GetUserByEmail / GetUserByToken gRPC 구현 |
| `AuthService` | 회원가입, 로그인, 로그아웃, 토큰 갱신/검증, 비밀번호 변경/재설정, 이메일 인증 |
| `UserService` | 프로필 조회/수정, 회원 탈퇴, gRPC용 사용자 조회 |
| `OAuthService` | PKCE 기반 OAuth2 플로우, 소셜 로그인, 소셜 계정 연결 |
| `JwtProvider` | JWT 생성·파싱·만료 검사, 클레임 추출 |
| `TokenBlacklistRepository` | 로그아웃/토큰 갱신 시 이전 토큰을 Redis에 블랙리스트 등록 |
| `EmailVerificationRepository` | 이메일 인증 코드(5분) 및 verified 상태(10분) 관리 |
| `PasswordResetRepository` | 비밀번호 재설정 토큰(30분) 및 이메일 양방향 매핑 관리 |
| `SessionInvalidationRepository` | 비밀번호 변경/탈퇴 시 무효화 시각 저장 — 이 시각 이전 토큰 전부 거부 |
| `SocialLinkTokenRepository` | OAuth 계정 연결 state UUID ↔ userId 매핑 (3분 TTL) |
| `OAuthApiClient` | Google Access Token 요청 및 UserInfo API 호출 |
| `ImageServiceClient` | Image 서비스 gRPC 클라이언트 (ActivateImage, ChangeImage) |

---

## 데이터베이스

**MySQL 스키마:** `flipnote_user`

### `users` 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 사용자 ID |
| email | VARCHAR(100) | UNIQUE, NOT NULL | 이메일 주소 |
| password | VARCHAR | NOT NULL | BCrypt 인코딩된 비밀번호 |
| name | VARCHAR(50) | NOT NULL | 실명 |
| nickname | VARCHAR(50) | NOT NULL | 닉네임 |
| profile_image_url | VARCHAR | DEFAULT (S3 기본 이미지) | 프로필 이미지 URL |
| phone | VARCHAR(20) | UNIQUE | 전화번호 |
| sms_agree | BOOLEAN | | SMS 수신 동의 |
| role | ENUM | DEFAULT 'USER' | USER / ADMIN |
| status | ENUM | DEFAULT 'ACTIVE' | ACTIVE / WITHDRAWN |
| deleted_at | DATETIME | NULLABLE | 탈퇴 시각 |
| created_at | DATETIME | NOT NULL | 생성 시각 |
| modified_at | DATETIME | NOT NULL | 수정 시각 |

### `oauth_links` 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 연결 ID |
| provider | VARCHAR | NOT NULL | OAuth 제공자 (예: "google") |
| provider_id | VARCHAR | NOT NULL | 제공자가 발급한 사용자 ID |
| user_id | BIGINT | FK → users.id | 연결된 사용자 |
| linked_at | DATETIME | | 연결 시각 |

**인덱스:** `(provider, provider_id)`

---

## Redis 키 구조

| Repository | 키 패턴 | 값 | TTL |
|-----------|--------|-----|-----|
| TokenBlacklist | `blacklist:{jti}` | `"true"` | 토큰 잔여 만료 시간 |
| EmailVerification | `email-verification:{email}` | 6자리 코드 | 5분 |
| EmailVerification | `email-verified:{email}` | `"verified"` | 10분 |
| PasswordReset | `password-reset:token:{token}` | email | 30분 |
| PasswordReset | `password-reset:email:{email}` | token | 30분 |
| SessionInvalidation | `session-invalidation:{userId}` | epochMillis | 영구 (탈퇴 후 정리 불필요) |
| SocialLinkToken | `social-link-token:{state}` | userId | 3분 |

---

## 외부 의존성

### gRPC — 서비스 노출 (포트 9091)

```
UserQueryService (user_query.proto)
  ├── GetUser(user_id) → {id, email, nickname, profile_image_url}
  ├── GetUsers(user_ids[]) → {users[]}
  ├── GetUserByEmail(email) → {exists, user}
  └── GetUserByToken(access_token) → {user_id, nickname}
```

**호출하는 서비스:** Cardset, Group, Reaction (사용자 정보 조회)

### gRPC — 서비스 호출 (Image 서비스, 포트 9092)

```
ImageCommandService (image.proto)
  ├── ActivateImage(image_ref_id, reference_type, reference_id) → {url}
  └── ChangeImage(reference_type, reference_id, image_ref_id) → {image_ref_id, url}
```

프로필 이미지 등록/변경 시 호출.

### 외부 HTTP

| 대상 | 용도 |
|------|------|
| Google OAuth2 (`accounts.google.com`) | Authorization URI 생성 |
| Google Token API (`oauth2.googleapis.com`) | Access Token 요청 |
| Google UserInfo API (`googleapis.com/oauth2/v3/userinfo`) | 사용자 이메일/프로필 조회 |

### 이메일 발송

`ApplicationEventPublisher`를 통해 이메일 발송 이벤트를 발행. 이메일 발송 자체는 비동기 처리 (인증 코드, 비밀번호 재설정 링크).

---

## 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `JWT_SECRET` | (필수) | JWT 서명 비밀키 |
| `JWT_ACCESS_EXPIRATION` | `900000` (15분) | Access Token 만료 시간 (ms) |
| `JWT_REFRESH_EXPIRATION` | `604800000` (7일) | Refresh Token 만료 시간 (ms) |
| `GOOGLE_CLIENT_ID` | (필수) | Google OAuth2 클라이언트 ID |
| `GOOGLE_CLIENT_SECRET` | (필수) | Google OAuth2 클라이언트 Secret |
| `OAUTH_BASE_URL` | `https://api3.flipnote.site` | OAuth 콜백 base URL |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/flipnote_user` | DB URL |
| `SPRING_DATASOURCE_USERNAME` | `root` | DB 사용자 |
| `SPRING_DATASOURCE_PASSWORD` | `root` | DB 비밀번호 |
| `SPRING_REDIS_HOST` | `localhost` | Redis 호스트 |
| `SPRING_REDIS_PORT` | `6379` | Redis 포트 |
| `APP_CLIENT_URL` | `http://localhost:3000` | 프론트엔드 클라이언트 URL (OAuth 리다이렉트용) |

---

## 주요 흐름

### 회원가입 플로우

```
Client
  │ POST /v1/auth/email-verification/request {email}
  │   → 6자리 코드 생성 → Redis 저장 5분 → 이메일 발송
  │
  │ POST /v1/auth/email-verification {email, code}
  │   → 코드 검증 → Redis에 "verified" 마크 10분
  │
  │ POST /v1/auth/register {email, password, name, ...}
  │   → Redis "verified" 확인
  │   → 중복 이메일 체크
  │   → BCrypt 인코딩
  │   → users 테이블 INSERT
  │   → {userId} 반환
```

### 토큰 검증 플로우 (Gateway 경유)

```
Client (쿠키: accessToken)
  → Gateway AuthenticationFilter
      │ POST user-service:8081/v1/auth/token/validate {token}
      │   → JwtProvider.parseClaims() (서명 검증)
      │   → TokenBlacklist 확인
      │   → SessionInvalidation 시각 확인
      │   → {userId, email, role} 반환
      → Gateway가 X-User-Id, X-User-Email, X-User-Role 헤더 추가
  → 대상 서비스 (User, Reaction, Group 등)
```

### 세션 무효화 메커니즘

```
비밀번호 변경 또는 회원 탈퇴 시:
  → SessionInvalidationRepository.save(userId, now)

이후 모든 토큰 검증 시:
  → 토큰의 iat(발급 시각) < sessionInvalidatedAt 이면 401 거부
  → 블랙리스트 등록 없이도 기존 모든 토큰이 자동 무효화
```

### OAuth2 소셜 로그인 플로우 (PKCE)

```
Client
  │ GET /oauth2/authorization/google
  │   → codeVerifier 생성 → SHA-256 → codeChallenge
  │   → codeVerifier를 HttpOnly 쿠키로 저장 (3분)
  │   → Google 인증 URI로 302 리다이렉트
  │
  │ [Google 로그인 완료]
  │
  │ GET /oauth2/callback/google?code=...
  │   → codeVerifier 쿠키 추출
  │   → Google에서 Access Token 요청 (code + codeVerifier)
  │   → Google UserInfo API 호출
  │   → OAuthLink에서 사용자 매핑
  │   → JWT 토큰 쌍 발급
  │   → 클라이언트 /social-login/success 로 리다이렉트
```
