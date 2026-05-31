# API Gateway — Architecture

## 서비스 목적 및 책임

모든 외부 HTTP/WebSocket 요청의 단일 진입점이다. JWT 인증 필터, 라우팅, CORS 처리를 담당한다. 각 마이크로서비스는 외부에 포트를 직접 노출하지 않으며, 반드시 이 Gateway를 통해서만 접근한다.

- **JWT 인증**: 쿠키(`accessToken`)에서 토큰을 추출하고 User 서비스에 유효성 검증 위임
- **헤더 주입**: 인증 성공 시 `X-User-Id`, `X-User-Email`, `X-User-Role` 헤더를 다운스트림 요청에 추가
- **헤더 위조 방지**: 클라이언트가 전달한 `X-User-*` 헤더를 모든 요청에서 최우선으로 제거
- **라우팅**: 경로 패턴에 따라 요청을 해당 마이크로서비스로 프록시
- **CORS**: 모든 Origin 허용, credentialed 요청 지원

---

## 기술 스택

| 항목 | 버전/구현 |
|---|---|
| Spring Boot | 3.4.1 |
| Java | 21 |
| Spring Cloud Gateway | 2024.0.0 |
| Spring WebFlux | Reactive 기반 (Non-blocking) |
| Resilience4j | Circuit Breaker (의존성 포함, 미설정) |

---

## 디렉토리 구조

```
FlipNote-Gateway/
├── src/main/java/flipnote/apigateway/
│   ├── ApiGatewayApplication.java
│   ├── client/
│   │   └── TokenValidationClient.java     # User 서비스에 토큰 검증 요청
│   ├── config/
│   │   └── CorsConfig.java                # CORS WebFilter 설정
│   └── filter/
│       ├── AuthenticationFilter.java      # 인증 필요 라우트에 적용되는 GatewayFilter
│       └── HeaderCleanupGlobalFilter.java # X-User-* 헤더 위조 방지 GlobalFilter
└── src/main/resources/
    ├── application.yml                    # 운영 라우팅 설정 (K8s 서비스명 사용)
    └── application-local.yml              # 로컬 개발용 라우팅 설정 (localhost 사용)
```

---

## 핵심 클래스/파일

| 파일 | 역할 |
|---|---|
| `AuthenticationFilter.java` | `AbstractGatewayFilterFactory` 구현. 쿠키에서 `accessToken` 추출 → User 서비스 검증 → `X-User-*` 헤더 주입. 실패 시 401 반환 |
| `HeaderCleanupGlobalFilter.java` | `GlobalFilter` + `Ordered.HIGHEST_PRECEDENCE`. 모든 요청에서 `X-User-Id`, `X-User-Email`, `X-User-Role` 제거 (AuthenticationFilter보다 먼저 실행) |
| `TokenValidationClient.java` | WebClient로 `POST /v1/auth/token/validate` 호출. 응답에서 `userId`, `email`, `role` 파싱 |
| `CorsConfig.java` | 모든 Origin, 모든 헤더, GET/POST/PUT/DELETE/PATCH/OPTIONS 허용. `X-User-*` 헤더 Expose |
| `application.yml` | Spring Cloud Gateway 라우팅 규칙 전체 정의 |

---

## 인증 흐름

```
Client
  │
  │  HTTP Request (Cookie: accessToken=<jwt>)
  ▼
HeaderCleanupGlobalFilter        ← X-User-* 헤더 제거 (위조 방지)
  │
  ▼
AuthenticationFilter (인증 필요 라우트만)
  │  POST /v1/auth/token/validate  →  User Service
  │  ←  { userId, email, role }
  │
  │  request 헤더 추가:
  │    X-User-Id: {userId}
  │    X-User-Email: {email}
  │    X-User-Role: {role}
  ▼
Downstream Microservice
```

---

## 라우팅 테이블

### User Service (→ user-service:8081)

| Route ID | 경로 패턴 | 인증 |
|---|---|---|
| auth-public | `/v1/auth/login`, `/v1/auth/register`, `/v1/auth/token/refresh`, `/v1/auth/email-verification/request`, `/v1/auth/email-verification`, `/v1/auth/password-reset/request`, `/v1/auth/password-reset`, `/oauth2/callback/*`, `/oauth2/authorization/*` | Public |
| auth-private | `/v1/auth/logout`, `/v1/auth/password`, `/v1/auth/social-links`, `/v1/auth/social-links/*`, `/v1/oauth2/links/*` | Bearer JWT |
| user-private | `/v1/users/me`, `/v1/users/{userId}`, `/v1/users` | Bearer JWT |
| user-swagger | `/users/swagger-ui.html`, `/users/swagger-ui/**`, `/users/v3/api-docs`, `/users/v3/api-docs/**` | Public |

### Image Service (→ image-service:8082)

| Route ID | 경로 패턴 | 인증 |
|---|---|---|
| image-public | `/v1/images/upload` | Public |
| image-private | `/v1/images/**` | Bearer JWT |

### Reaction Service (→ reaction-service:8083)

| Route ID | 경로 패턴 | 인증 |
|---|---|---|
| reaction-private | `/v1/likes/{targetType}/{targetId}`, `/v1/likes/{targetType}`, `/v1/bookmarks/{targetType}/{targetId}`, `/v1/bookmarks/{targetType}` | Bearer JWT |

### Group Service (→ group-service:8084)

| Route ID | 경로 패턴 | 인증 |
|---|---|---|
| group | `/v1/groups/**`, `/v1/joins/**`, `/v1/group-invitations` | Bearer JWT |
| group-swagger | `/groups/swagger-ui.html`, `/groups/swagger-ui/**`, `/groups/v3/api-docs`, `/groups/v3/api-docs/**` | Public |
| group-health | `/groups/actuator/health` | Public |

### Cardset Service (→ cardset-service:8085)

| Route ID | 경로 패턴 | 인증 | 프로토콜 |
|---|---|---|---|
| group-cardsets | `/v1/groups/*/card-sets` | Bearer JWT | HTTP |
| cardset-private | `/v1/card-sets`, `/v1/card-sets/**`, `/v1/cards`, `/v1/cards/**` | Bearer JWT | HTTP |
| cardset-websocket | `/v1/card-sets/ws/**` | Bearer JWT | WebSocket (`ws://`) |
| cardset-swagger | `/card-sets/swagger-ui.html`, `/card-sets/swagger-ui/**`, `/card-sets/v3/api-docs`, `/card-sets/v3/api-docs/**` | Public | HTTP |
| cardset-health | `/card-sets/health` | Public | HTTP |

### Notification Service (→ notification-service:8086)

| Route ID | 경로 패턴 | 인증 |
|---|---|---|
| notification-private | `/v1/notifications/**` | Bearer JWT |
| notification-swagger | `/notifications/swagger-ui.html`, `/notifications/swagger-ui/**`, `/notifications/v3/api-docs`, `/notifications/v3/api-docs/**` | Public |

> **주의**: `group-cardsets` 라우트(`/v1/groups/*/card-sets` → cardset-service)가 `group` 라우트(`/v1/groups/**` → group-service)보다 먼저 정의되어 있어, 카드셋 목록 조회는 cardset-service로 라우팅된다.

---

## CORS 설정

| 항목 | 값 |
|---|---|
| Allowed Origins | `*` (모든 Origin, 패턴 방식) |
| Allowed Methods | GET, POST, PUT, DELETE, PATCH, OPTIONS |
| Allowed Headers | `*` |
| Exposed Headers | `X-User-Id`, `X-User-Email`, `X-User-Role` |
| Allow Credentials | `true` |

---

## 환경 변수 (Kubernetes Secrets/ConfigMap)

| 변수 | 설명 |
|---|---|
| `app.user-service.url` | User 서비스 주소 (토큰 검증 호출용). 기본: `http://user-service:8081` |
| `app.image-service.url` | Image 서비스 주소 |
| `app.group-service.url` | Group 서비스 주소 |
| `app.cardset-service.url` | Cardset 서비스 주소 |
| `server.port` | Gateway 포트. 기본: `8080` |

Spring Cloud Gateway 라우팅은 `application.yml`의 `spring.cloud.gateway.routes` 에 정의되며, 런타임 환경(K8s/로컬)에 따라 `application-local.yml`로 URI를 오버라이드한다.

---

## Actuator

| 엔드포인트 | 경로 | 설명 |
|---|---|---|
| Health (liveness) | `/actuator/health/liveness` | K8s liveness probe |
| Health (readiness) | `/actuator/health/readiness` | K8s readiness probe |
