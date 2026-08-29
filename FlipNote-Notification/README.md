# 📔 FlipNote — Notification Service

**FlipNote 서비스의 알림 도메인 백엔드 레포지토리입니다.**

![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java_21-007396?logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?logo=mysql&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?logo=rabbitmq&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?logo=firebase&logoColor=black)
![Deploy](https://img.shields.io/badge/Deploy-GHCR%20%2B%20Docker-2496ED?logo=docker&logoColor=white)

---

## 📑 목차

- [시작하기](#시작하기)
- [환경 변수](#환경-변수)
- [실행 및 배포](#실행-및-배포)
- [프로젝트 구조](#프로젝트-구조)

---

<a id="시작하기"></a>

## 🚀 시작하기

### 사전 요구사항

- **Java** 21 이상
- **Gradle** 8 이상
- **MySQL** 8 이상
- **RabbitMQ** 3 이상
- Firebase 프로젝트 생성 및 서비스 계정 JSON 발급

### 설치

```bash
# 의존성 설치 및 빌드
./gradlew build -x test
```

---

<a id="환경-변수"></a>

## 🔐 환경 변수

`application.yaml`에서 참조하는 환경 변수 목록입니다.

```text
# ─── Database ───────────────────────────────────────────
DB_URL=jdbc:mysql://localhost:3306/flipnote_notification
DB_USERNAME=
DB_PASSWORD=

# ─── JPA ────────────────────────────────────────────────
# create | create-drop | update | validate | none
JPA_DDL_AUTO=validate

# ─── RabbitMQ ───────────────────────────────────────────
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# ─── Firebase ───────────────────────────────────────────
# 서비스 계정 JSON 전체 내용 (문자열)
FIREBASE_SERVICE_ACCOUNT_JSON=

# ─── Server ─────────────────────────────────────────────
SERVER_PORT=8086

# ─── Async Thread Pool ──────────────────────────────────
ASYNC_CORE_POOL_SIZE=4
ASYNC_MAX_POOL_SIZE=10
ASYNC_QUEUE_CAPACITY=100

# ─── Swagger ────────────────────────────────────────────
SPRINGDOC_SERVER_URL=http://localhost:8086
```

> **⚠️ 주의**: 환경 변수 파일은 절대 git에 커밋하지 마세요. `.gitignore`에 포함되어 있는지 반드시 확인하세요.

---

<a id="실행-및-배포"></a>

## 🖥️ 실행 및 배포

### 로컬 개발 서버 실행

```bash
./gradlew bootRun
```

기본적으로 `http://localhost:8086`에서 실행됩니다.
Swagger UI는 `http://localhost:8086/notifications/swagger-ui.html`에서 확인할 수 있습니다.

### 프로덕션 빌드

```bash
./gradlew bootJar
```

`build/libs/notification-0.0.1-SNAPSHOT.jar` 파일이 생성됩니다.

### 테스트 실행

```bash
./gradlew test
```

### Docker 이미지 빌드 및 실행

```bash
# 이미지 빌드
docker build -t flipnote-notification .

# 컨테이너 실행
docker run -p 8086:8086 \
  -e DB_URL=... \
  -e FIREBASE_SERVICE_ACCOUNT_JSON=... \
  flipnote-notification
```

### 배포 (GitHub Actions)

`main` 브랜치에 push 시 GitHub Actions가 자동으로 아래 과정을 실행합니다.

**CI** (`push` / `pull_request` → `main`)
1. JDK 21 설치
2. `./gradlew build -x test` — 빌드 검증
3. `./gradlew test` — 테스트 실행
4. Dependency-Check — 취약점 분석 리포트 생성

**CD** (`push` → `main`)
1. GitHub Container Registry(GHCR) 로그인
2. Docker 이미지 빌드
3. `ghcr.io/dungbik/flipnote-notification` 이미지 Push

> 배포에 필요한 시크릿(`ORG_PAT`)은 GitHub Repository → Settings → Secrets and variables → Actions에 등록해야 합니다.

---

<a id="프로젝트-구조"></a>

## 📁 프로젝트 구조

- 간략화 버전

    ```text
    src/main/java/flipnote/notification/
    ├── domain/          # 도메인 레이어 (엔티티, 레포지토리, 에러코드)
    ├── application/     # 애플리케이션 레이어 (서비스, 커맨드, 결과 객체)
    ├── infrastructure/  # 인프라 레이어 (Firebase, RabbitMQ, 설정)
    └── interfaces/      # 인터페이스 레이어 (HTTP 진입점)
    ```

```text
FlipNote-Notification/
├── src/
│   ├── main/
│   │   ├── java/flipnote/notification/
│   │   │   ├── FlipNoteNotificationApplication.java
│   │   │   │
│   │   │   ├── domain/                                # 도메인 레이어
│   │   │   │   ├── common/                            # 도메인 공통
│   │   │   │   │   ├── ErrorCode.java
│   │   │   │   │   ├── BizException.java
│   │   │   │   │   ├── CommonErrorCode.java
│   │   │   │   │   └── BaseEntity.java
│   │   │   │   ├── notification/                      # 알림 도메인
│   │   │   │   │   ├── Notification.java
│   │   │   │   │   ├── NotificationType.java
│   │   │   │   │   ├── NotificationRepository.java
│   │   │   │   │   └── NotificationErrorCode.java
│   │   │   │   └── fcmtoken/                          # FCM 토큰 도메인
│   │   │   │       ├── FcmToken.java
│   │   │   │       └── FcmTokenRepository.java
│   │   │   │
│   │   │   ├── application/                           # 애플리케이션 레이어
│   │   │   │   ├── dto/
│   │   │   │   │   ├── command/                       # 서비스 입력 커맨드 (검증 어노테이션 없음)
│   │   │   │   │   │   └── NotificationListCommand.java
│   │   │   │   │   └── result/                        # 서비스 출력 결과 객체 (프로토콜 무관)
│   │   │   │   │       ├── FcmSendResult.java
│   │   │   │   │       ├── NotificationResult.java
│   │   │   │   │       └── PagedResult.java
│   │   │   │   ├── FcmSender.java                     # FCM 아웃바운드 포트 인터페이스
│   │   │   │   ├── FcmTokenService.java
│   │   │   │   ├── NotificationCommandService.java
│   │   │   │   └── NotificationQueryService.java
│   │   │   │
│   │   │   ├── infrastructure/                        # 인프라 레이어
│   │   │   │   ├── config/                            # 범용 설정
│   │   │   │   │   ├── AppConfig.java
│   │   │   │   │   ├── AsyncConfig.java
│   │   │   │   │   ├── AsyncProperties.java
│   │   │   │   │   └── SwaggerConfig.java
│   │   │   │   ├── persistence/                       # JPA 설정 및 변환기
│   │   │   │   │   ├── JpaAuditingConfig.java
│   │   │   │   │   └── MapToJsonConverter.java
│   │   │   │   ├── fcm/                               # Firebase Cloud Messaging 어댑터
│   │   │   │   │   ├── FirebaseConfig.java
│   │   │   │   │   ├── FirebaseFcmSender.java         # FcmSender 구현체
│   │   │   │   │   └── FcmErrorCode.java
│   │   │   │   └── messaging/                         # RabbitMQ 메시지 처리
│   │   │   │       ├── RabbitMQConfig.java
│   │   │   │       ├── GroupInviteMessage.java
│   │   │   │       ├── GroupInviteMessageListener.java
│   │   │   │       ├── GroupJoinRequestMessage.java
│   │   │   │       └── GroupJoinRequestMessageListener.java
│   │   │   │
│   │   │   └── interfaces/                            # 인터페이스 레이어
│   │   │       └── http/                              # HTTP 진입점
│   │   │           ├── NotificationController.java
│   │   │           ├── NotificationControllerDocs.java
│   │   │           ├── dto/request/                   # HTTP Request DTO (@Valid 포함)
│   │   │           │   ├── NotificationListRequest.java
│   │   │           │   └── TokenRegisterRequest.java
│   │   │           └── common/                        # HTTP 공통 유틸
│   │   │               ├── ApiResponse.java
│   │   │               ├── ApiResponseAdvice.java
│   │   │               ├── CursorPagingRequest.java
│   │   │               ├── CursorPagingResponse.java
│   │   │               ├── GlobalExceptionHandler.java
│   │   │               └── HttpHeaders.java
│   │   │
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── messages.properties                    # 알림 메시지 템플릿
│   │
│   └── test/
│       └── java/flipnote/notification/
│
├── Dockerfile
├── build.gradle.kts
└── settings.gradle.kts
```
