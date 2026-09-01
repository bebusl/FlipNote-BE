# FlipNote 시스템 아키텍처 개요

## 전체 구조 다이어그램

```
                          ┌─────────────────────────────────────────────────────┐
                          │                  Client (Browser / App)              │
                          └────────────────────────┬────────────────────────────┘
                                                   │ HTTPS / WSS
                                                   ▼
                          ┌─────────────────────────────────────────────────────┐
                          │           API Gateway  :8080  (api3.flipnote.site)  │
                          │    JWT 인증 필터 · 라우팅 · CORS · 헤더 위조 방지     │
                          └──┬──────┬───────┬────────┬──────────┬───────────────┘
                             │HTTP  │HTTP   │HTTP    │HTTP      │ WS
                             ▼      ▼       ▼        ▼          ▼
              ┌──────────────┐  ┌───────┐ ┌──────┐ ┌─────────┐ ┌───────────────┐
              │ User  :8081  │  │Image  │ │React-│ │ Group   │ │  Cardset      │
              │              │  │:8082  │ │ion   │ │ :8084   │ │  :8085        │
              │ MySQL·Redis  │  │MySQL  │ │:8083 │ │ MySQL   │ │  MySQL·Redis  │
              │              │  │AWS S3 │ │MySQL │ │ RabbitMQ│ │  RabbitMQ     │
              └──────────────┘  └───────┘ │RabbitMQ └──────┘  │  Socket.IO+Yjs│
                                          └──────┘            └───────────────┘
                                                                      │
                          ┌─────────────────────────────────────────────────────┐
                          │         Notification  :8086                         │
                          │         MySQL · Firebase FCM                         │
                          └─────────────────────────────────────────────────────┘
```

---

## 서비스 목록

| 서비스 | HTTP | gRPC | 기술 스택 | 역할 |
|---|---|---|---|---|
| API Gateway | 8080 | — | Spring Boot 3 / Java 21, Spring Cloud Gateway | 단일 진입점. JWT 인증, 라우팅, CORS |
| User | 8081 | 9091 | Spring Boot 3 / Java 21, MySQL, Redis | 회원가입/로그인, OAuth2(Google), JWT 발급·검증, 이메일 인증 |
| Image | 8082 | 9092 | Spring Boot 3 / Java 17, MySQL, AWS S3 | S3 Presigned URL 발급, 이미지-엔티티 연결 |
| Reaction | 8083 | 9093 | Spring Boot 3 / Java 21, MySQL, RabbitMQ | 좋아요, 북마크 |
| Group | 8084 | 9094 | Spring Boot 3 / Java 17, MySQL, RabbitMQ | 그룹 생성·관리, 멤버, 초대, 권한 |
| Cardset | 8085 | 9095 | NestJS / TypeScript, MySQL, Redis, RabbitMQ | 카드셋·카드 CRUD, 실시간 협업 편집 (Socket.IO + Yjs) |
| Notification | 8086 | — | Spring Boot 3 / Java 21, MySQL, Firebase FCM | FCM 푸시 알림, FCM 토큰 관리 |

---

## 서비스 간 통신

### gRPC 의존 그래프 (동기 호출)

```
Cardset  ──→ User     :9091  (카드셋 생성자·멤버 정보 조회)
         ──→ Group    :9094  (그룹 소속 여부·이름 확인)
         ──→ Image    :9092  (이미지 URL 활성화)
         ──→ Reaction :9093  (카드셋별 반응 수 조회)

Reaction ──→ Cardset  :9095  (카드셋 존재 여부 확인)

Group    ──→ User     :9091  (멤버 프로필 조회)
         ──→ Image    :9092  (그룹 이미지 활성화)

User     ──→ Image    :9092  (프로필 이미지 등록·변경)
```

### RabbitMQ 이벤트 흐름 (비동기)

```
Group ──[group.invite.exchange]──────→ Notification
       (그룹 초대 이벤트)               (FCM 푸시 발송)

Group ──[group.join.request.exchange]─→ Notification
       (그룹 가입 요청 이벤트)           (FCM 푸시 발송)

Reaction ──[reaction.exchange]────────→ Cardset
           (좋아요/북마크 이벤트)         (반응 카운트 동기화)
```

### WebSocket

Cardset 서비스가 Socket.IO + Yjs CRDT로 실시간 협업 편집 채널을 제공한다.  
Gateway는 `/v1/card-sets/ws/**` 경로를 `ws://cardset-service:8085` 로 업그레이드 프록시한다.

---

## 인증 흐름

```
Client (Cookie: accessToken)
  │
  ▼
API Gateway
  │  HeaderCleanupGlobalFilter  — X-User-* 헤더 위조 방지 (HIGHEST_PRECEDENCE)
  │  AuthenticationFilter       — POST /v1/auth/token/validate → User Service
  │                               응답: { userId, email, role }
  │  헤더 주입: X-User-Id / X-User-Email / X-User-Role
  ▼
각 마이크로서비스 (헤더에서 사용자 식별, JWT 직접 검증 없음)
```

Public 경로(`/v1/auth/login`, `/v1/auth/register` 등)는 `AuthenticationFilter`를 거치지 않는다.

---

## 공유 인프라

| 인프라 | 사용 서비스 | 용도 |
|---|---|---|
| MySQL | 전 서비스 (각자 독립 스키마) | 영속 데이터 저장 |
| Redis | User, Cardset | User: 토큰 블랙리스트·이메일 인증·세션 무효화 / Cardset: WebSocket 세션 |
| RabbitMQ | Group, Reaction, Cardset, Notification | 서비스 간 비동기 이벤트 버스 |
| AWS S3 | Image | 원본 이미지 저장, Presigned URL 발급 |
| Firebase FCM | Notification | 모바일·웹 푸시 알림 발송 |

---

## 배포 구조

```
소스 변경
  │
  ▼
GitHub Actions (CI)
  ├── 빌드 & 테스트
  └── Docker 이미지 빌드 → GHCR (ghcr.io/bebusl/flipnote-{service})
                                        │
                                        ▼
                              ArgoCD (GitOps)
                                ├── FlipNote-Infra 레포 감지
                                ├── Helm Chart 렌더링
                                │     charts/apps/{service}/values.yaml
                                └── Kubernetes 클러스터에 배포
                                      ├── Namespace: flipnote
                                      ├── Ingress (Nginx): api3.flipnote.site
                                      ├── External Secrets Operator
                                      └── Vault → K8s Secret 주입
```

| 항목 | 내용 |
|---|---|
| 컨테이너 레지스트리 | GHCR (`ghcr.io/bebusl/flipnote-*`) |
| 패키지 매니저 | Helm |
| CD 도구 | ArgoCD (자동 동기화 + self-heal) |
| 시크릿 관리 | HashiCorp Vault + External Secrets Operator |
| Ingress | Nginx (`api3.flipnote.site`) |
| CI | GitHub Actions (`.github/workflows/ci.yml`) |
| CD | GitHub Actions → image push → ArgoCD 감지 (`.github/workflows/cd.yml`) |

---

## 관련 문서

- [API Gateway](gateway.md)
- [User Service](user-service.md)
- [Image Service](image-service.md)
- [Reaction Service](reaction-service.md)
- [Group Service](group-service.md)
- [Cardset Service](cardset-service.md)
- [Notification Service](notification-service.md)
