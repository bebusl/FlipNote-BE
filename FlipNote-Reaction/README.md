# 📒 FlipNote — Reaction Service

**FlipNote 서비스의 반응(좋아요·북마크) 도메인 백엔드 레포지토리입니다.**

![Spring Boot](https://img.shields.io/badge/Spring_Boot_4-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java_21-007396?logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?logo=mysql&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?logo=rabbitmq&logoColor=white)
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

### 설치

```bash
# 의존성 설치 및 빌드
./gradlew build -x test
```

---

<a id="환경-변수"></a>

## 🔐 환경 변수

`application.yml`에서 참조하는 환경 변수 목록입니다. 로컬 실행 시 `.env` 또는 IDE 실행 구성에 아래 변수를 설정합니다.

```text
# ─── Database ───────────────────────────────────────────
DB_URL=jdbc:mysql://localhost:3306/flipnote_reaction
DB_USERNAME=
DB_PASSWORD=
# create | create-drop | update | validate | none
DDL_AUTO=validate

# ─── RabbitMQ ───────────────────────────────────────────
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# ─── gRPC ───────────────────────────────────────────────
# Reaction 서비스 gRPC 서버 포트 (기본값 9093)
GRPC_SERVER_PORT=9093
# CardSet 서비스 gRPC 클라이언트 주소 (기본값 static://cardset-service:9095)
GRPC_CARDSET_ADDRESS=static://localhost:9095
```

> **⚠️ 주의**: 환경 변수 파일은 절대 git에 커밋하지 마세요. `.gitignore`에 포함되어 있는지 반드시 확인하세요.

---

<a id="실행-및-배포"></a>

## 🖥️ 실행 및 배포

### 로컬 개발 서버 실행

```bash
./gradlew bootRun
```

기본적으로 `http://localhost:8083`에서 실행됩니다.
gRPC 서버는 기본적으로 `9093` 포트에서 실행됩니다.

### 프로덕션 빌드

```bash
./gradlew bootJar
```

`build/libs/reaction-0.0.1-SNAPSHOT.jar` 파일이 생성됩니다.

### 테스트 실행

```bash
./gradlew test
```

### Docker 이미지 빌드 및 실행

```bash
# 이미지 빌드
docker build -t flipnote-reaction .

# 컨테이너 실행
docker run -p 8083:8083 -p 9093:9093 \
  -e DB_URL=... \
  -e RABBITMQ_HOST=... \
  flipnote-reaction
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
3. `ghcr.io/dungbik/flipnote-reaction` 이미지 Push

> 배포에 필요한 시크릿(`ORG_PAT`)은 GitHub Repository → Settings → Secrets and variables → Actions에 등록해야 합니다.

---

<a id="프로젝트-구조"></a>

## 📁 프로젝트 구조

- 간략화 버전

    ```text
    src/main/java/flipnote/reaction/
    ├── domain/          # 도메인 레이어 (엔티티, 레포지토리, 도메인 이벤트, 에러코드)
    ├── application/     # 애플리케이션 레이어 (서비스, 조회 컴포넌트, Result 객체)
    ├── infrastructure/  # 인프라 레이어 (영속성, 메시징, gRPC 클라이언트)
    └── interfaces/      # 인터페이스 레이어 (HTTP, gRPC 진입점)
    ```

```text
FlipNote-Reaction/
├── src/
│   ├── main/
│   │   ├── java/flipnote/reaction/
│   │   │   ├── ReactionApplication.java
│   │   │   │
│   │   │   ├── domain/                                      # 도메인 레이어
│   │   │   │   ├── common/                                  # 도메인 공통
│   │   │   │   │   ├── ErrorCode.java
│   │   │   │   │   ├── BizException.java
│   │   │   │   │   ├── CommonErrorCode.java
│   │   │   │   │   └── BaseEntity.java
│   │   │   │   ├── bookmark/                                # 북마크 도메인
│   │   │   │   │   ├── Bookmark.java
│   │   │   │   │   ├── BookmarkTargetType.java
│   │   │   │   │   ├── BookmarkRepository.java
│   │   │   │   │   ├── BookmarkErrorCode.java
│   │   │   │   │   └── event/                               # 북마크 도메인 이벤트
│   │   │   │   │       ├── BookmarkAddedEvent.java
│   │   │   │   │       └── BookmarkRemovedEvent.java
│   │   │   │   └── like/                                    # 좋아요 도메인
│   │   │   │       ├── Like.java
│   │   │   │       ├── LikeTargetType.java
│   │   │   │       ├── LikeRepository.java
│   │   │   │       ├── LikeErrorCode.java
│   │   │   │       └── event/                               # 좋아요 도메인 이벤트
│   │   │   │           ├── LikeAddedEvent.java
│   │   │   │           └── LikeRemovedEvent.java
│   │   │   │
│   │   │   ├── application/                                 # 애플리케이션 레이어
│   │   │   │   ├── common/
│   │   │   │   │   └── CardSetSummaryResult.java            # 공유 Result 객체
│   │   │   │   ├── bookmark/
│   │   │   │   │   ├── BookmarkService.java
│   │   │   │   │   ├── BookmarkReader.java
│   │   │   │   │   └── BookmarkResult.java                  # 서비스 반환 Result
│   │   │   │   └── like/
│   │   │   │       ├── LikeService.java
│   │   │   │       ├── LikeReader.java
│   │   │   │       └── LikeResult.java                      # 서비스 반환 Result
│   │   │   │
│   │   │   ├── infrastructure/                              # 인프라 레이어
│   │   │   │   ├── grpc/                                    # gRPC 클라이언트
│   │   │   │   │   ├── GrpcConfig.java
│   │   │   │   │   └── CardSetGrpcClient.java
│   │   │   │   ├── messaging/                               # 메시지 발행 + 설정
│   │   │   │   │   ├── RabbitMqConfig.java
│   │   │   │   │   ├── ReactionMessage.java
│   │   │   │   │   ├── ReactionEventPublisher.java
│   │   │   │   │   └── ReactionEventListener.java           # @TransactionalEventListener
│   │   │   │   └── persistence/                             # 영속성 어댑터 + JPA 설정
│   │   │   │       ├── AuditingConfig.java
│   │   │   │       ├── BookmarkRepositoryAdapter.java
│   │   │   │       ├── SpringDataBookmarkRepository.java
│   │   │   │       ├── LikeRepositoryAdapter.java
│   │   │   │       └── SpringDataLikeRepository.java
│   │   │   │
│   │   │   └── interfaces/                                  # 인터페이스 레이어
│   │   │       ├── http/                                    # HTTP 진입점
│   │   │       │   ├── BookmarkController.java
│   │   │       │   ├── LikeController.java
│   │   │       │   ├── common/                              # 공통 응답, 예외 처리
│   │   │       │   │   ├── ApiResponse.java
│   │   │       │   │   ├── ApiResponseAdvice.java
│   │   │       │   │   ├── GlobalExceptionHandler.java
│   │   │       │   │   ├── HeaderConstants.java
│   │   │       │   │   ├── IdResponse.java
│   │   │       │   │   ├── PagingRequest.java
│   │   │       │   │   └── PagingResponse.java
│   │   │       │   └── dto/request/
│   │   │       │       ├── BookmarkSearchRequest.java
│   │   │       │       ├── BookmarkTargetTypeRequest.java
│   │   │       │       ├── LikeSearchRequest.java
│   │   │       │       └── LikeTargetTypeRequest.java
│   │   │       └── grpc/                                    # gRPC 서버 진입점
│   │   │           ├── ReactionGrpcService.java
│   │   │           └── GrpcExceptionHandlerImpl.java
│   │   │
│   │   └── resources/
│   │       └── application.yml
│   │
│   └── test/
│       └── java/flipnote/reaction/
│
├── Dockerfile
├── build.gradle.kts
└── settings.gradle.kts
```
