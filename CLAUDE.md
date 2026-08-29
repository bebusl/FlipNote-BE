# FlipNote-BE — CLAUDE.md

## 프로젝트 개요

FlipNote는 실시간 협업이 가능한 플래시카드 서비스다. 백엔드는 7개의 독립 마이크로서비스와 API Gateway로 구성된 MSA 구조다.

### 서비스 목록

| 서비스 | HTTP 포트 | gRPC 포트 | 기술 스택 | 역할 |
|---|---|---|---|---|
| API Gateway | 8080 | — | Spring Boot 21, Spring Cloud Gateway | JWT 인증 필터, 라우팅, CORS |
| User | 8081 | 9091 | Spring Boot 21, MySQL, Redis | 회원가입/로그인, OAuth2(Google), JWT, 이메일 인증 |
| Image | 8082 | 9092 | Spring Boot 17, MySQL, AWS S3 | 이미지 업로드, Presigned URL, S3 연동 |
| Reaction | 8083 | 9093 | Spring Boot 21, MySQL, RabbitMQ | 좋아요, 북마크 |
| Group | 8084 | 9094 | Spring Boot 17, MySQL, RabbitMQ | 그룹 생성/관리, 멤버, 초대, 권한 |
| Cardset | 8085 | 9095 | NestJS/TypeScript 22, MySQL, Redis, RabbitMQ | 카드셋/카드 CRUD, 실시간 협업(Socket.IO + Yjs) |
| Notification | 8086 | — | Spring Boot 21, MySQL, Firebase FCM | 푸시 알림, FCM 토큰 관리 |

### 서비스 간 통신

- **gRPC (동기)**: 서비스 간 데이터 조회. 각 서비스는 `.proto` 파일로 계약을 정의한다.
- **RabbitMQ (비동기)**: 이벤트 기반 알림. Group → Notification (초대/가입 요청), Reaction → Cardset (반응 이벤트).
- **WebSocket**: Cardset 서비스의 실시간 협업 편집 (Socket.IO).
- **HTTP (외부)**: 모든 클라이언트 요청은 Gateway(8080)를 통해 진행. 서비스는 외부에 HTTP를 직접 노출하지 않는다.

### gRPC 의존 관계

```
Cardset  → User(9091), Group(9094), Image(9092), Reaction(9093)
Reaction → Cardset(9095)
Group    → User(9091), Image(9092)
User     → Image(9092)
```

---

## Claude 사용 목적

이 레포에서 Claude는 **두 가지 목적**으로만 사용한다.

### 1. 기존 구조 파악

코드를 분석해 서비스별 아키텍처, 폴더 구조, 의존성을 `/docs/architecture/` 에 마크다운으로 정리한다.

### 2. API Docs 최신화

각 서비스의 소스 코드(컨트롤러, DTO)를 읽어 실제 코드 기준으로 API 문서를 생성하거나 갱신한다.  
문서는 두 가지 형식으로 `/docs/api/` 하위에 작성한다.

---

## 문서 구조

```
/docs/
├── api/
│   ├── ai/                   # LLM이 파싱하기 좋은 구조화된 API 문서 (영어)
│   │   ├── user-service.md
│   │   ├── cardset-service.md
│   │   ├── reaction-service.md
│   │   ├── group-service.md
│   │   ├── image-service.md
│   │   └── notification-service.md
│   └── human/                # 인간이 읽기 좋은 한국어 API 문서
│       ├── user-service.md
│       ├── cardset-service.md
│       ├── reaction-service.md
│       ├── group-service.md
│       ├── image-service.md
│       └── notification-service.md
└── architecture/
    ├── overview.md            # 전체 시스템 아키텍처 개요 (서비스맵, 통신 흐름)
    ├── gateway.md
    ├── user-service.md
    ├── cardset-service.md
    ├── reaction-service.md
    ├── group-service.md
    ├── image-service.md
    └── notification-service.md
```

---

## 문서 형식 가이드

### `/docs/api/ai/` — LLM-friendly API 문서

**목적**: LLM이 API 스펙을 빠르고 정확하게 파싱할 수 있도록 구조화.

**규칙**:
- 영어로 작성
- 파일 상단에 YAML frontmatter로 서비스 메타 정보 기술
- 각 엔드포인트는 고정된 블록 구조로 표현 (아래 형식 준수)
- 타입은 `string`, `number`, `boolean`, `string[]`, `enum(A|B|C)`, `object` 등으로 명확히 표기
- required/optional 구분 명시
- 인증 방식: `Public` / `Bearer JWT` / `Bearer JWT (Gateway strips, X-User-Id forwarded)`
- Enum 값은 모두 나열
- gRPC 서비스 메서드도 마지막에 포함

**엔드포인트 블록 형식**:

```
## {METHOD} {path}

**Auth:** {Public | Bearer JWT}
**Description:** {one-line summary}

**Path Params:**
  {name}: {type} — {description}

**Query Params:**
  {name}: {type} (optional) — {description}

**Request Body:**
  {field}: {type} (required|optional) — {description}

**Response {status}:**
  {field}: {type} — {description}

**Errors:**
  {status} {ERROR_CODE} — {description}
```

**예시**:

```
## POST /v1/auth/login

**Auth:** Public
**Description:** Authenticate user with email and password, returns JWT tokens.

**Request Body:**
  email: string (required) — user email address
  password: string (required, min 8 chars) — user password

**Response 200:**
  accessToken: string — JWT access token (short-lived)
  refreshToken: string — JWT refresh token (long-lived)

**Errors:**
  401 INVALID_CREDENTIALS — wrong email or password
  404 USER_NOT_FOUND — email not registered
```

---

### `/docs/api/human/` — Human-friendly API 문서 (한국어)

**목적**: 프론트엔드 개발자가 빠르게 읽고 API를 사용할 수 있도록 작성.

**규칙**:
- **한국어**로 작성
- 파라미터는 테이블로 정리
- 실제 `curl` 예시 또는 JSON 요청/응답 예시 포함
- 인증 필요 여부, 필요한 권한 조건 설명
- 에러 상황과 해결 방법 설명
- 파일 상단에 서비스 설명과 Base URL 명시

**예시**:

```markdown
## 로그인

이메일과 비밀번호로 로그인합니다. 성공 시 Access Token과 Refresh Token을 반환합니다.

**POST** `/v1/auth/login`  
인증 불필요

### 요청

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | string | ✓ | 이메일 주소 |
| password | string | ✓ | 비밀번호 (최소 8자) |

### 응답 (200)

\`\`\`json
{
  "accessToken": "eyJhbGciOiJI...",
  "refreshToken": "eyJhbGciOiJI..."
}
\`\`\`

### 에러

| 코드 | 설명 |
|------|------|
| 401 | 이메일 또는 비밀번호가 올바르지 않음 |
| 404 | 등록되지 않은 이메일 |
```

---

### `/docs/architecture/` — 아키텍처 문서

**`overview.md`** 포함 내용:
- 전체 시스템 ASCII 아키텍처 다이어그램
- 각 서비스의 역할 한 줄 요약
- 서비스 간 통신 방식 (gRPC 의존 그래프, RabbitMQ 이벤트 흐름)
- 공유 인프라 (MySQL, Redis, RabbitMQ, AWS S3, Firebase FCM)
- 배포 구조 (Docker, GHCR, ArgoCD, Kubernetes, Vault)

**서비스별 `.md`** 포함 내용:
- 서비스 목적 및 책임
- 디렉토리 구조 (계층형 표기)
- 핵심 클래스/파일 목록 및 역할
- 데이터베이스 주요 테이블/엔티티
- 외부 의존성 (gRPC 호출, 메시지 발행/구독)
- 환경 변수 목록

---

## API 문서 작성 프로세스

Claude가 특정 서비스의 API 문서를 생성하거나 갱신할 때 따를 순서:

1. **Controller 파일 읽기** — 아래 경로에서 HTTP 엔드포인트를 파악
2. **DTO/Request/Response 파일 읽기** — 필드 타입, 유효성 검사 조건 확인
3. **Gateway 라우팅 확인** — `FlipNote-Gateway/src/main/resources/application.yml` 에서 외부 경로 매핑 확인
4. **기존 참고자료 확인** — Swagger 어노테이션(`@Operation`, `@ApiResponse`), 기존 `API.md` 파일
5. **ai/ 와 human/ 두 버전 모두 작성** — 동일 서비스 문서를 두 형식으로 각각 저장

---

## 서비스별 핵심 파일 위치

### Controller 파일 (HTTP 엔드포인트 정의)

| 서비스 | Controller 경로 |
|---|---|
| User | `FlipNote-User/src/main/java/flipnote/user/interfaces/http/` |
| Image | `FlipNote-Image/src/main/java/flipnote/image/adapter/in/web/` |
| Reaction | `FlipNote-Reaction/src/main/java/flipnote/reaction/interfaces/http/` |
| Group | `FlipNote-Group/src/main/java/flipnote/group/adapter/in/web/` |
| Cardset | `FlipNote-Cardset/src/cardset/infrastructure/http/*.controller.ts` |
| Notification | `FlipNote-Notification/src/main/java/flipnote/notification/interfaces/http/` |

### Gateway 라우팅

- `FlipNote-Gateway/src/main/resources/application.yml` — 외부 경로 → 내부 서비스 매핑 정의

### 기타 참고 파일

| 서비스 | 참고 파일 |
|---|---|
| Notification | `FlipNote-Notification/API.md` — 상세 API 스펙 |
| Cardset | `FlipNote-Cardset/src/cardset/infrastructure/http/*.dto.ts` — DTO 타입 |
| Java 서비스 공통 | `*Docs.java` 파일 — Swagger 명세 인터페이스 (예: `NotificationControllerDocs.java`) |

### Proto 파일 (gRPC 서비스 계약)

gRPC 서비스 메서드와 메시지 타입은 `.proto` 파일이 소스 오브 트루스다.

| 서비스 | Proto 디렉토리 |
|---|---|
| Java 서비스 (User/Image/Reaction/Group) | `{서비스}/src/main/proto/` |
| NestJS Cardset | `FlipNote-Cardset/src/proto/` |

각 서비스의 proto 디렉토리에는 **자신이 노출하는 서비스 정의**와 **자신이 호출하는 타 서비스 정의**가 함께 들어 있다.

---

## 인프라 구조 파악 위치

| 항목 | 경로 |
|---|---|
| ArgoCD 앱 정의 | `FlipNote-Infra/argocd/apps/` |
| Kubernetes Helm values | `FlipNote-Infra/charts/apps/{service}/values.yaml` |
| 외부 시크릿 설정 | `FlipNote-Infra/argocd/infra/external-secrets.yaml` |
| Vault 설정 | `FlipNote-Infra/argocd/infra/vault-config.yaml` |
| CI 워크플로 | `각 서비스/.github/workflows/ci.yml` |
| CD 워크플로 | `각 서비스/.github/workflows/cd.yml` |
