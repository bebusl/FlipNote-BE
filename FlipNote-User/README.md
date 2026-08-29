# 📒 FlipNote — User Service

**FlipNote 서비스의 유저 도메인 백엔드 레포지토리입니다.**

![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java_21-007396?logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-FF4438?logo=redis&logoColor=white)
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
- **Redis** 6 이상
- Google OAuth2 클라이언트 생성 및 API 키 발급
- Resend 계정 생성 및 API 키 발급

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
DB_URL=jdbc:mysql://localhost:3306/flipnote_user
DB_USERNAME=
DB_PASSWORD=

# ─── Redis ──────────────────────────────────────────────
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# ─── JPA ────────────────────────────────────────────────
# create | create-drop | update | validate | none
DDL_AUTO=update

# ─── gRPC ───────────────────────────────────────────────
GRPC_PORT=9092

# ─── JWT ────────────────────────────────────────────────
JWT_SECRET=
# 액세스 토큰 만료 시간 (ms), 기본값 900000 (15분)
JWT_ACCESS_EXPIRATION=900000
# 리프레시 토큰 만료 시간 (ms), 기본값 604800000 (7일)
JWT_REFRESH_EXPIRATION=604800000

# ─── Email (Resend) ─────────────────────────────────────
APP_RESEND_API_KEY=

# ─── Client ─────────────────────────────────────────────
# 프론트엔드 URL (CORS, 리다이렉트에 사용)
APP_CLIENT_URL=http://localhost:3000

# ─── Google OAuth2 ──────────────────────────────────────
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
```

> **⚠️ 주의**: 환경 변수 파일은 절대 git에 커밋하지 마세요. `.gitignore`에 포함되어 있는지 반드시 확인하세요.

---

<a id="실행-및-배포"></a>

## 🖥️ 실행 및 배포

### 로컬 개발 서버 실행

```bash
./gradlew bootRun
```

기본적으로 `http://localhost:8081`에서 실행됩니다.
Swagger UI는 `http://localhost:8081/users/swagger-ui.html`에서 확인할 수 있습니다.

### 프로덕션 빌드

```bash
./gradlew bootJar
```

`build/libs/user-0.0.1-SNAPSHOT.jar` 파일이 생성됩니다.

### 테스트 실행

```bash
./gradlew test
```

### Docker 이미지 빌드 및 실행

```bash
# 이미지 빌드
docker build -t flipnote-user .

# 컨테이너 실행
docker run -p 8081:8081 \
  -e DB_URL=... \
  -e JWT_SECRET=... \
  flipnote-user
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
3. `ghcr.io/dungbik/flipnote-user` 이미지 Push

> 배포에 필요한 시크릿(`ORG_PAT`)은 GitHub Repository → Settings → Secrets and variables → Actions에 등록해야 합니다.

---

<a id="프로젝트-구조"></a>

## 📁 프로젝트 구조

- 간략화 버전

    ```text
    src/main/java/flipnote/user/
    ├── domain/          # 도메인 레이어 (엔티티, 레포지토리, 에러코드, 이벤트)
    ├── application/     # 애플리케이션 레이어 (서비스, 커맨드, 결과 객체)
    ├── infrastructure/  # 인프라 레이어 (JWT, Redis, 메일, OAuth, 설정)
    └── interfaces/      # 인터페이스 레이어 (HTTP, gRPC 진입점)
    ```

```text
FlipNote-User/
├── src/
│   ├── main/
│   │   ├── java/flipnote/user/
│   │   │   ├── UserApplication.java
│   │   │   │
│   │   │   ├── domain/                                # 도메인 레이어
│   │   │   │   ├── common/                            # 도메인 공통
│   │   │   │   │   ├── ErrorCode.java
│   │   │   │   │   ├── BizException.java
│   │   │   │   │   └── EmailSendException.java
│   │   │   │   ├── entity/                            # JPA 엔티티
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── OAuthLink.java
│   │   │   │   │   └── BaseEntity.java
│   │   │   │   ├── repository/                        # 레포지토리 인터페이스
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   └── OAuthLinkRepository.java
│   │   │   │   ├── event/                             # 도메인 이벤트
│   │   │   │   │   ├── EmailVerificationSendEvent.java
│   │   │   │   │   └── PasswordResetCreateEvent.java
│   │   │   │   ├── AuthErrorCode.java
│   │   │   │   ├── UserErrorCode.java
│   │   │   │   ├── ImageErrorCode.java
│   │   │   │   ├── TokenClaims.java
│   │   │   │   ├── TokenPair.java
│   │   │   │   ├── PasswordResetConstants.java
│   │   │   │   └── VerificationConstants.java
│   │   │   │
│   │   │   ├── application/                           # 애플리케이션 레이어
│   │   │   │   ├── command/                           # 서비스 입력 커맨드 (검증 어노테이션 없음)
│   │   │   │   │   ├── SignupCommand.java
│   │   │   │   │   ├── LoginCommand.java
│   │   │   │   │   ├── ChangePasswordCommand.java
│   │   │   │   │   └── UpdateProfileCommand.java
│   │   │   │   ├── result/                            # 서비스 출력 결과 객체 (프로토콜 무관)
│   │   │   │   │   ├── UserResult.java
│   │   │   │   │   ├── UserRegisterResult.java
│   │   │   │   │   ├── MyInfoResult.java
│   │   │   │   │   ├── UserInfoResult.java
│   │   │   │   │   ├── UserUpdateResult.java
│   │   │   │   │   ├── TokenValidateResult.java
│   │   │   │   │   ├── SocialLinkResult.java
│   │   │   │   │   └── SocialLinksResult.java
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── OAuthService.java
│   │   │   │   └── UserService.java
│   │   │   │
│   │   │   ├── infrastructure/                        # 인프라 레이어
│   │   │   │   ├── config/                            # 범용 설정 (App, JPA, Swagger, gRPC 클라이언트)
│   │   │   │   ├── jwt/                               # JWT 발급/검증 + 설정
│   │   │   │   ├── mail/                              # 메일 발송 서비스 + 설정 + 코드 생성
│   │   │   │   ├── oauth/                             # Google OAuth2 클라이언트 + 설정
│   │   │   │   ├── redis/                             # Redis 저장소 (토큰, 인증코드 등)
│   │   │   │   └── listener/                          # 도메인 이벤트 리스너
│   │   │   │
│   │   │   └── interfaces/                            # 인터페이스 레이어
│   │   │       ├── http/                              # HTTP 진입점
│   │   │       │   ├── AuthController.java            # 인증 (회원가입, 로그인, 비밀번호 등)
│   │   │       │   ├── OAuthController.java           # 소셜 로그인 (Google OAuth2)
│   │   │       │   ├── UserController.java            # 유저 정보 조회/수정
│   │   │       │   ├── dto/request/                   # HTTP Request DTO (@Valid 포함)
│   │   │       │   └── common/                        # ApiResponse, 예외 처리, 쿠키 유틸
│   │   │       └── grpc/                              # gRPC 진입점
│   │   │           ├── GrpcUserQueryService.java      # 유저 조회 gRPC 서비스
│   │   │           └── GrpcExceptionHandlerImpl.java  # gRPC 전역 예외 처리
│   │   │
│   │   ├── proto/                                     # gRPC proto 파일
│   │   │   ├── user_query.proto
│   │   │   └── image.proto
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       └── templates/email/                       # 이메일 HTML 템플릿 (Thymeleaf)
│   │           ├── email-verification.html
│   │           └── password-reset.html
│   │
│   └── test/
│       └── java/flipnote/user/
│
├── Dockerfile
├── build.gradle.kts
└── settings.gradle.kts
```
