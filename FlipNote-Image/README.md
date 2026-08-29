# 📒 FlipNote — FlipNote-Image

**FlipNote 서비스의 이미지 백엔드 레포지토리입니다.**

![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java_17-ED8B00?logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-FF4438?logo=redis&logoColor=white)
![AWS S3](https://img.shields.io/badge/AWS_S3-569A31?logo=amazons3&logoColor=white)
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
- Redis
- AWS S3 버킷
- gRPC 서비스 (Group) 실행 중

### 설치

```bash
./gradlew build
```

---

## 🔐 환경 변수

아래 환경 변수를 시스템 또는 배포 환경에 설정합니다.

```yaml
# ─── 데이터베이스 ──────────────────────────────────────
DB_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=

# ─── Redis ────────────────────────────────────────────
SPRING_DATA_REDIS_HOST=
SPRING_DATA_REDIS_PORT=
SPRING_DATA_REDIS_PASSWORD=

# ─── AWS S3 ───────────────────────────────────────────
S3_BUCKET_NAME=
S3_BUCKET_REGION=
S3_ACCESS_KEY=
S3_SECRET_KEY=
```

---

## 🖥️ 실행 및 배포

### 로컬 개발 서버 실행

```bash
./gradlew bootRun
```

- HTTP API: `http://localhost:8082`
- gRPC: `localhost:9092`

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

| Secret    | 설명                               |
| --------- | ---------------------------------- |
| `ORG_PAT` | GHCR push용 Personal Access Token  |

---

## 📁 프로젝트 구조

<details>
<summary>간략화 버전</summary>

```
src/main/java/flipnote/image/
├── adapter/       # 입출력 어댑터 (REST 컨트롤러, gRPC 엔드포인트, 영속성, S3)
├── api/           # 공통 응답 형식, DTO
├── application/   # 유스케이스, 커맨드/결과 포트, 서비스
├── domain/        # 도메인 모델, 정책
├── global/        # 설정, 상수
└── infrastructure/ # S3, QueryDSL 리포지토리
```
</details>

<details>
<summary>상세 구조 보기</summary>

```
src/main/java/flipnote/image/
├── ImageApplication.java                    # 애플리케이션 진입점
│
├── adapter/
│   ├── in/
│   │   ├── grpc/
│   │   │   └── ImageCommandGrpcService.java # gRPC 서버 엔드포인트
│   │   └── web/
│   │       └── ImageController.java         # REST 컨트롤러
│   └── out/
│       ├── S3ImageStorageAdapter.java        # S3 이미지 업로드/삭제
│       ├── other/
│       │   └── DefaultImageAdapter.java      # 기본 이미지 URL 제공
│       ├── persistence/
│       │   ├── ImageRefRepositoryAdapter.java
│       │   ├── ImageRepositoryAdapter.java
│       │   └── ImageS3KeyAdapter.java
│       └── storage/s3/
│           ├── S3ObjectMetadataAdapter.java  # S3 객체 메타데이터 조회
│           ├── S3PresignedUrlAdapter.java    # Presigned URL 생성
│           └── S3PublicUrlAdapter.java       # 공개 URL 생성
│
├── api/
│   └── dto/
│       ├── request/
│       │   └── IssuePresignedUrlRequestDto.java
│       └── response/
│           └── IssuePresignedUrlResponseDto.java
│
├── application/
│   ├── port/
│   │   ├── in/                              # 유스케이스 인터페이스
│   │   │   ├── ActivateImageUseCase.java
│   │   │   ├── ChangeImageUseCase.java
│   │   │   ├── DeleteImageUseCase.java
│   │   │   ├── GetImageUrlByReferenceUseCase.java
│   │   │   ├── IssuePresignedUrlUseCase.java
│   │   │   ├── command/
│   │   │   │   └── IssuePresignedUrlCommand.java
│   │   │   └── result/
│   │   │       ├── ChangeImageResult.java
│   │   │       └── IssuePresignedUrlResult.java
│   │   └── out/                             # 출력 포트 인터페이스
│   │       ├── DefaultImagePort.java
│   │       ├── ImagePort.java
│   │       ├── ImageRefPort.java
│   │       ├── ImageS3KeyPort.java
│   │       ├── ImageStoragePort.java
│   │       ├── ObjectMetadataPort.java
│   │       ├── PresignedUrlPort.java
│   │       └── PublicUrlPort.java
│   └── service/                             # 유스케이스 구현체
│       ├── ActivateImageService.java
│       ├── ChangeImageService.java
│       ├── DeleteImageService.java
│       ├── GetImageUrlByReferenceService.java
│       └── IssuePresignedUrlService.java
│
├── domain/
│   ├── model/
│   │   ├── BaseEntity.java
│   │   ├── image/
│   │   │   ├── Image.java                   # 이미지 핵심 도메인 (hash, s3Key, mimeType 등)
│   │   │   └── ImageMeta.java
│   │   └── reference/
│   │       ├── ImageRef.java                # 이미지-참조 연결 (유저/그룹/카드셋)
│   │       ├── Reference.java
│   │       └── ReferenceType.java           # USER | GROUP | CARD_SET
│   └── policy/
│       └── ImageNamingPolicy.java
│
└── infrastructure/
    ├── config/
    │   ├── AuditingConfig.java
    │   ├── QuerydslConfig.java
    │   └── S3Config.java
    ├── persistence/
    │   ├── jpa/
    │   │   ├── ImageRepository.java
    │   │   └── ImageRefRepository.java
    │   └── querydsl/
    │       ├── ImageRepositoryCustom.java
    │       ├── ImageRepositoryImpl.java
    │       ├── ImageRefRepositoryCustom.java
    │       └── ImageRefRepositoryImpl.java
    └── s3/
        (AWS S3 클라이언트 설정)

src/main/proto/
├── image.proto   # ImageCommandService (URL 조회, 이미지 활성화/변경/삭제)
└── group.proto   # GroupCommandService (그룹명 조회)

src/main/resources/
└── application.yml
```

</details>