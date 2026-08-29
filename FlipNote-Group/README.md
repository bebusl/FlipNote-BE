# 📒 FlipNote — FlipNote-Group

**FlipNote 서비스의 그룹 백엔드 레포지토리입니다.**

![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java_17-ED8B00?logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?logo=mysql&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?logo=rabbitmq&logoColor=white)
![gRPC](https://img.shields.io/badge/gRPC-244C5A?logo=google&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)
![QueryDSL](https://img.shields.io/badge/QueryDSL-0769AD?logoColor=white)

---

## 📑 목차

- [시작하기](#-시작하기)
- [환경 변수](#-환경-변수)
- [실행 및 배포](#-실행-및-배포)
- [프로젝트 구조](#-프로젝트-구조)

---

## 🚀 시작하기

### 사전 요구사항

- **Java** 17 이상
- **Gradle** 8 이상
- MySQL 8
- RabbitMQ
- gRPC 서비스 (User · Image) 실행 중

### 설치

```bash
./gradlew build
```

---

## 🔐 환경 변수

아래 환경 변수를 시스템 또는 배포 환경에 설정합니다.

```application.yml
# ─── 데이터베이스 ──────────────────────────────────────
SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=

# ─── RabbitMQ ─────────────────────────────────────────
RABBITMQ_HOST=
RABBITMQ_PORT=
RABBITMQ_USERNAME=
RABBITMQ_PASSWORD=

# ─── gRPC 서비스 URL ───────────────────────────────────
GRPC_USER_URL=
GRPC_IMAGE_URL=

# ─── 이메일 (Resend) ───────────────────────────────────
RESEND_API_KEY=
RESEND_FROM_EMAIL=

# ─── 기본 이미지 URL ───────────────────────────────────
# 그룹 기본 이미지로 사용할 URL로 변경하세요
IMAGE_DEFAULT_GROUP=

# ─── 기타 ─────────────────────────────────────────────
APP_CLIENT_URL=
```

---

## 🖥️ 실행 및 배포

### 로컬 개발 서버 실행

```bash
./gradlew bootRun
```

- HTTP API: `http://localhost:8084`
- gRPC: `localhost:9094`
- Swagger: `http://localhost:8084/groups/swagger-ui.html`

### 프로덕션 빌드

```bash
./gradlew bootJar
```

### 배포 (GitHub Actions)

`main` 브랜치에 push 시 GitHub Actions가 자동으로 아래 과정을 실행합니다.

#### CD (`cd.yml`) — `main` push 시 실행

1. Docker 이미지 빌드
2. GitHub Container Registry(`ghcr.io`)에 push
3. Slack 알림 (성공/실패)

> 배포에 필요한 시크릿은 GitHub Repository → Settings → Secrets and variables → Actions에 등록해야 합니다.

| Secret      | 설명                              |
| ----------- | --------------------------------- |
| `ORG_PAT`   | GHCR push용 Personal Access Token |

---

## 📁 프로젝트 구조

<details>
<summary>간략화 버전</summary>

```
src/main/java/flipnote/group/
├── adapter/       # 입출력 어댑터 (REST 컨트롤러, gRPC 엔드포인트, 영속성)
├── api/           # 공통 응답 형식, 예외 핸들러, DTO
├── application/   # 유스케이스, 커맨드/결과 포트, 서비스
├── domain/        # 도메인 모델, 정책, 이벤트
├── global/        # 설정, 상수, 응답 래퍼
└── infrastructure/ # 이메일, 메시징(RabbitMQ), QueryDSL 리포지토리
```
</details>

<details>
<summary>상세 구조 보기</summary>

```
src/main/java/flipnote/group/
├── GroupApplication.java               # 애플리케이션 진입점
│
├── adapter/
│   ├── in/
│   │   ├── grpc/
│   │   │   └── GroupCommandService.java  # gRPC 서버 엔드포인트
│   │   └── web/                          # REST 컨트롤러
│   │       ├── GroupController.java
│   │       ├── InviteController.java
│   │       ├── JoinController.java
│   │       ├── MeController.java
│   │       ├── MemberController.java
│   │       └── PermissionController.java
│   └── out/
│       ├── entity/                       # JPA 엔티티
│       │   ├── GroupEntity.java
│       │   ├── GroupMemberEntity.java
│       │   ├── InviteEntity.java
│       │   ├── JoinEntity.java
│       │   ├── PermissionEntity.java
│       │   └── RoleEntity.java
│       └── persistence/                  # 포트 구현체 (리포지토리 어댑터)
│           ├── GroupRepositoryAdapter.java
│           ├── GroupMemberRepositoryAdapter.java
│           ├── GroupRoleRepositoryAdapter.java
│           ├── InviteRepositoryAdapter.java
│           └── JoinRepositoryAdapter.java
│
├── api/
│   ├── advice/
│   │   └── GlobalExceptionHandler.java
│   └── dto/
│       ├── request/                      # 요청 DTO (23개)
│       └── response/                     # 응답 DTO (17개)
│
├── application/
│   ├── dto/                              # 메시지 DTO (RabbitMQ)
│   ├── listener/
│   │   └── GuestInviteEventListener.java
│   ├── port/
│   │   ├── in/                           # 유스케이스 인터페이스
│   │   │   ├── command/                  # 커맨드 객체 (29개)
│   │   │   └── result/                   # 결과 객체 (17개)
│   │   └── out/                          # 출력 포트 인터페이스
│   │       ├── GroupRepositoryPort.java
│   │       ├── GroupMemberRepositoryPort.java
│   │       ├── GroupRoleRepositoryPort.java
│   │       ├── InviteRepositoryPort.java
│   │       ├── JoinRepositoryPort.java
│   │       └── NotificationMessagePort.java
│   └── service/                          # 유스케이스 구현체 (23개)
│
├── domain/
│   ├── event/
│   │   └── GuestInviteCreatedEvent.java
│   ├── model/
│   │   ├── group/                        # Group, Category, Visibility, JoinPolicy
│   │   ├── invite/                       # InviteInfo, InviteStatus, InviteResponseStatus
│   │   ├── join/                         # JoinInfo, JoinStatus
│   │   ├── member/                       # MemberInfo, GroupMemberRole
│   │   ├── permission/                   # GroupPermission
│   │   └── role/                         # GroupRole
│   └── policy/
│       ├── BusinessException.java
│       └── ErrorCode.java
│
├── global/
│   ├── config/                           # 설정 (Async, Auditing, gRPC, QueryDSL, Swagger 등)
│   ├── constants/
│   │   └── HttpConstants.java
│   └── response/
│       ├── ApiResponse.java
│       └── ApiResponseAdvice.java
│
└── infrastructure/
    ├── email/
    │   ├── EmailService.java
    │   └── ResendEmailService.java       # Resend 이메일 발송 구현체
    ├── messaging/
    │   ├── RabbitMQConfig.java
    │   └── GroupJoinRequestMessageProducer.java
    └── persistence/
        ├── jpa/                          # Spring Data JPA 리포지토리
        └── querydsl/                     # QueryDSL 커스텀 쿼리 구현체

src/main/proto/
├── group.proto   # GroupCommandService (그룹명 조회, 멤버십 확인, 내 그룹 목록)
├── image.proto   # ImageCommandService (이미지 URL 조회/변경)
└── user.proto    # UserQueryService (유저 조회)

src/main/resources/
├── application.yml
└── templates/email/
    └── guest-group-invitation.html       # 게스트 초대 이메일 템플릿
```

</details>
