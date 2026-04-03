# 📒 FlipNote — FlipNote-Cardset

**FlipNote 서비스의 카드셋 백엔드 레포지토리입니다.**

![NestJS](https://img.shields.io/badge/NestJS-E0234E?logo=nestjs&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?logo=typescript&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white)
![Socket.io](https://img.shields.io/badge/Socket.io-010101?logo=socket.io&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)
![gRPC](https://img.shields.io/badge/gRPC-244C5A?logo=google&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?logo=rabbitmq&logoColor=white)

---

## 📑 목차

- [시작하기](#-시작하기)
- [환경 변수](#-환경-변수)
- [실행 및 배포](#-실행-및-배포)
- [프로젝트 구조](#-프로젝트-구조)

---

## 🚀 시작하기

### 사전 요구사항

- **Node.js** 22 이상
- **npm** 10 이상
- MySQL 8
- Redis
- RabbitMQ
- gRPC 서비스 (User · Group · Image · Reaction) 실행 중

### 설치

```bash
npm install
```

---

## 🔐 환경 변수

프로젝트 루트에 `.env` 파일을 생성하고 아래 변수를 설정합니다.

```env
# ─── 데이터베이스 ──────────────────────────────────────
DB_HOST=
DB_PORT=
DB_USERNAME=
DB_PASSWORD=
DB_DATABASE=
DB_SYNCHRONIZE=

# ─── Redis ────────────────────────────────────────────
REDIS_HOST=
REDIS_PORT=
REDIS_PASSWORD=

# ─── 서버 ─────────────────────────────────────────────
PORT=8085
GRPC_PORT=9095
NODE_ENV=production

# ─── gRPC 서비스 URL ───────────────────────────────────
GROUP_GRPC_URL=
USER_GRPC_URL=
IMAGE_GRPC_URL=
GRPC_REACTION_URL=

# ─── 기타 ─────────────────────────────────────────────
DEFAULT_CARDSET_IMAGE_URL=
RABBITMQ_URL=

# gRPC / WebSocket 인증 스킵 (로컬 테스트용)
SKIP_WS_AUTH=false
SKIP_USER_GRPC=false
SKIP_REACTION_GRPC=false
```

> **⚠️ 주의**: `.env` 파일은 절대 git에 커밋하지 마세요.

---

## 🖥️ 실행 및 배포

### 로컬 개발 서버 실행

```bash
npm run start:dev
```

- HTTP API: `http://localhost:8085`
- gRPC: `localhost:9095`
- Swagger: `http://localhost:8085/card-sets/swagger-ui`

### 프로덕션 빌드

```bash
npm run build
```

### 배포 (GitHub Actions)

`main` 브랜치에 push 시 GitHub Actions가 자동으로 아래 과정을 실행합니다.

#### CI (`ci.yml`) — push 및 PR 시 실행

1. `npm ci` — 의존성 설치
2. `npm run build` — 빌드 검증
3. `npm run lint` — 린트
4. `npm test` / `npm run test:e2e` — 테스트
5. Slack 알림 (성공/실패)

#### CD (`cd.yml`) — `main` push 시 실행

1. Docker 이미지 빌드
2. GitHub Container Registry(`ghcr.io`)에 push
3. Slack 알림 (성공/실패)

> 배포에 필요한 시크릿은 GitHub Repository → Settings → Secrets and variables → Actions에 등록해야 합니다.

| Secret              | 설명                              |
| ------------------- | --------------------------------- |
| `DB_HOST`           | CI용 DB 호스트                    |
| `DB_PORT`           | CI용 DB 포트                      |
| `DB_USERNAME`       | CI용 DB 유저명                    |
| `DB_PASSWORD`       | CI용 DB 비밀번호                  |
| `DB_DATABASE`       | CI용 DB 이름                      |
| `REDIS_HOST`        | CI용 Redis 호스트                 |
| `REDIS_PORT`        | CI용 Redis 포트                   |
| `ORG_PAT`           | GHCR push용 Personal Access Token |
| `SLACK_WEBHOOK_URL` | Slack 알림 Webhook URL            |

---

## 📁 프로젝트 구조


<details>
<summary>간략화 버전</summary>

```
src/
├── auth/          # WebSocket 인증 (gRPC 토큰 검증)
├── cardset/       # 카드셋·카드 도메인 (CRUD, 조회, 권한)
├── collaboration/ # 실시간 협업 (Socket.IO + Yjs)
├── reaction/      # 좋아요·북마크 이벤트 소비 (RabbitMQ)
├── shared/        # 공통 모듈 (응답 형식, 예외, gRPC 클라이언트)
└── proto/         # gRPC proto 파일
```
</details>

<details>
<summary>상세 구조 보기</summary>

```
src/
├── auth/                          # WebSocket 인증 (JWT / gRPC 토큰 검증)
│   ├── domain/
│   │   └── auth.service.ts
│   └── infrastructure/
│       └── guard/
│           └── ws-auth.guard.ts
│
├── cardset/                       # 카드셋 도메인
│   ├── application/               # Use Case, DTO
│   │   ├── cardset.use-case.ts
│   │   ├── card.use-case.ts
│   │   └── dto/
│   ├── domain/                    # 도메인 모델, 리포지토리 인터페이스
│   │   ├── model/                 # Cardset, Card, CardsetManager, Visibility
│   │   ├── repository/
│   │   └── service/
│   └── infrastructure/
│       ├── grpc/                  # gRPC 클라이언트 (User / Group / Image / Reaction)
│       ├── http/                  # REST 컨트롤러
│       └── persistence/           # TypeORM 구현체, ORM 엔티티, 매퍼
│
├── collaboration/                 # 실시간 협업 (Yjs + Socket.IO)
│   ├── application/
│   │   └── collaboration.use-case.ts
│   └── infrastructure/
│       ├── gateway/
│       │   └── collaboration.gateway.ts   # WebSocket 게이트웨이
│       ├── persistence/           # cardset_content, cardset_incrementals
│       └── redis/
│           └── yjs-document.service.ts    # Yjs 문서 Redis 관리
│
├── reaction/                      # RabbitMQ 좋아요/북마크 이벤트 소비
│
├── shared/                        # 공통 모듈
│   ├── common/                    # ApiResponse, BusinessException, ErrorCode
│   ├── config/                    # 설정 (auth)
│   ├── decorator/                 # @WsUser 데코레이터
│   ├── grpc/                      # GrpcClientModule
│   └── types/                     # UserAuth 타입
│
├── proto/                         # gRPC proto 파일
│   ├── user.proto
│   ├── group.proto
│   ├── image.proto
│   └── reaction.proto
│
├── app.module.ts
└── main.ts
```

</details>
