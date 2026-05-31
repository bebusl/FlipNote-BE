# API Gateway

**Base URL:** `http://localhost:8080` (로컬) / `http://gateway:8080` (K8s)

모든 클라이언트 요청의 단일 진입점입니다. Gateway 자체는 비즈니스 API를 제공하지 않으며, 경로 패턴에 따라 각 마이크로서비스로 요청을 프록시합니다. JWT 인증, CORS, 헤더 위조 방지를 담당합니다.

---

## 인증 방식

### 쿠키 기반 JWT

인증이 필요한 모든 API는 `accessToken` 쿠키를 요구합니다.

```
Cookie: accessToken=<JWT>
```

인증 처리 흐름:
1. 모든 요청에서 `X-User-Id`, `X-User-Email`, `X-User-Role` 헤더를 먼저 제거합니다 (헤더 위조 방지).
2. 인증 필요 경로: 쿠키에서 토큰을 추출하고 User 서비스에 검증을 요청합니다.
3. 검증 성공 시 다운스트림 서비스로 다음 헤더를 추가합니다:

| 헤더 | 타입 | 설명 |
|------|------|------|
| X-User-Id | number | 인증된 사용자 ID |
| X-User-Email | string | 인증된 사용자 이메일 |
| X-User-Role | string | 사용자 권한 (예: `ROLE_USER`) |

4. 쿠키 누락 또는 토큰 검증 실패 시 → **401 Unauthorized** (응답 바디 없음)

> 각 마이크로서비스는 `X-User-Id` 등의 헤더를 직접 읽어 사용자를 식별합니다. 프론트엔드는 이 헤더를 직접 다룰 필요가 없습니다.

---

## 라우팅 규칙

### User 서비스 (`user-service:8081`)

#### 인증 불필요 (Public)

| 경로 | 설명 |
|------|------|
| `POST /v1/auth/login` | 로그인 |
| `POST /v1/auth/register` | 회원가입 |
| `POST /v1/auth/token/refresh` | 액세스 토큰 갱신 |
| `POST /v1/auth/email-verification/request` | 이메일 인증 요청 |
| `POST /v1/auth/email-verification` | 이메일 인증 확인 |
| `POST /v1/auth/password-reset/request` | 비밀번호 재설정 요청 |
| `POST /v1/auth/password-reset` | 비밀번호 재설정 확인 |
| `GET /oauth2/callback/*` | OAuth2 콜백 |
| `GET /oauth2/authorization/*` | OAuth2 인증 시작 |

#### 인증 필요

| 경로 | 설명 |
|------|------|
| `POST /v1/auth/logout` | 로그아웃 |
| `PUT /v1/auth/password` | 비밀번호 변경 |
| `GET /v1/auth/social-links` | 소셜 계정 연동 목록 |
| `POST /v1/auth/social-links/*` | 소셜 계정 연동 |
| `DELETE /v1/auth/social-links/*` | 소셜 계정 연동 해제 |
| `GET /v1/oauth2/links/*` | OAuth2 링크 조회 |
| `GET /v1/users/me` | 내 정보 조회 |
| `GET/PUT /v1/users/{userId}` | 사용자 정보 조회/수정 |
| `DELETE /v1/users` | 회원 탈퇴 |

---

### Image 서비스 (`image-service:8082`)

| 경로 | 인증 | 설명 |
|------|------|------|
| `POST /v1/images/upload` | 불필요 | 이미지 업로드 (Presigned URL 발급) |
| `* /v1/images/**` | 필요 | 이미지 관련 나머지 API |

---

### Reaction 서비스 (`reaction-service:8083`)

모두 인증 필요.

| 경로 | 설명 |
|------|------|
| `GET/POST/DELETE /v1/likes/{targetType}/{targetId}` | 특정 대상 좋아요 |
| `GET /v1/likes/{targetType}` | 좋아요 목록 |
| `GET/POST/DELETE /v1/bookmarks/{targetType}/{targetId}` | 특정 대상 북마크 |
| `GET /v1/bookmarks/{targetType}` | 북마크 목록 |

---

### Group 서비스 (`group-service:8084`)

모두 인증 필요.

| 경로 | 설명 |
|------|------|
| `* /v1/groups/**` | 그룹 CRUD, 멤버 관리 등 |
| `* /v1/joins/**` | 그룹 가입 요청 |
| `* /v1/group-invitations` | 그룹 초대 |

> **예외**: `/v1/groups/{groupId}/card-sets` 경로는 Group 서비스가 아닌 **Cardset 서비스**로 라우팅됩니다. (아래 참고)

---

### Cardset 서비스 (`cardset-service:8085`)

모두 인증 필요.

| 경로 | 프로토콜 | 설명 |
|------|----------|------|
| `GET /v1/groups/*/card-sets` | HTTP | 그룹 내 카드셋 목록 조회 |
| `* /v1/card-sets` | HTTP | 카드셋 목록 조회 / 생성 |
| `* /v1/card-sets/**` | HTTP | 카드셋 상세, 수정, 삭제 등 |
| `* /v1/cards` | HTTP | 카드 생성 |
| `* /v1/cards/**` | HTTP | 카드 상세, 수정, 삭제 등 |
| `* /v1/card-sets/ws/**` | **WebSocket** | 실시간 협업 편집 (Socket.IO) |

#### WebSocket 연결 예시

```javascript
// Socket.IO 클라이언트 (브라우저)
const socket = io("http://localhost:8080", {
  path: "/v1/card-sets/ws",
  withCredentials: true  // accessToken 쿠키 자동 전송
});
```

---

### Notification 서비스 (`notification-service:8086`)

모두 인증 필요.

| 경로 | 설명 |
|------|------|
| `* /v1/notifications/**` | 알림 목록, 읽음 처리 등 |

---

## Swagger UI 접근 (개발용)

각 서비스의 Swagger UI는 Gateway를 통해 접근 가능합니다 (인증 불필요).

| 서비스 | Swagger UI URL |
|--------|----------------|
| User | `http://localhost:8080/users/swagger-ui.html` |
| Group | `http://localhost:8080/groups/swagger-ui.html` |
| Cardset | `http://localhost:8080/card-sets/swagger-ui.html` |
| Notification | `http://localhost:8080/notifications/swagger-ui.html` |

---

## CORS

모든 Origin에서의 credentialed 요청을 허용합니다.

| 항목 | 값 |
|------|-----|
| 허용 Origin | 모든 Origin (`*`) |
| 허용 메서드 | GET, POST, PUT, DELETE, PATCH, OPTIONS |
| 허용 헤더 | 모든 헤더 |
| 노출 헤더 | X-User-Id, X-User-Email, X-User-Role |
| Credentials 허용 | 예 (`withCredentials: true` 필요) |

---

## 에러 응답

Gateway 레벨에서 반환되는 에러 (응답 바디 없음):

| HTTP 상태 | 원인 |
|-----------|------|
| 401 | `accessToken` 쿠키 없음, 또는 토큰 검증 실패 |

서비스 레벨 에러(400, 403, 404, 500 등)는 각 마이크로서비스가 그대로 프록시하여 반환합니다.

---

## Health Check

| 경로 | 설명 |
|------|------|
| `GET /actuator/health` | Gateway 전체 헬스 |
| `GET /actuator/health/liveness` | Kubernetes liveness probe |
| `GET /actuator/health/readiness` | Kubernetes readiness probe |
