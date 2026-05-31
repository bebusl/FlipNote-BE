# Image Service — Architecture

## 서비스 목적

S3 Presigned URL 기반 이미지 업로드와 이미지 URL 해석을 담당한다.  
클라이언트는 Image 서비스에서 서명된 URL을 받아 S3에 직접 업로드하고, 서버 측에서 gRPC를 통해 이미지와 엔티티를 연결한다.

| 항목 | 값 |
|---|---|
| HTTP 포트 | 8082 |
| gRPC 포트 | 9092 |
| 기술 스택 | Spring Boot 17, MySQL, AWS S3 |

---

## 디렉토리 구조

```
FlipNote-Image/src/main/java/flipnote/image/
├── adapter/
│   ├── in/
│   │   ├── grpc/
│   │   │   └── ImageCommandGrpcService.java     # gRPC 서비스 구현체
│   │   └── web/
│   │       └── ImageController.java             # HTTP 컨트롤러 (Presigned URL 발급)
│   └── out/
│       ├── persistence/
│       │   ├── ImageRepositoryAdapter.java      # Image 영속성 어댑터
│       │   ├── ImageRefRepositoryAdapter.java   # ImageRef 영속성 어댑터
│       │   └── ImageS3KeyAdapter.java
│       ├── storage/s3/
│       │   ├── S3PresignedUrlAdapter.java       # S3 Presigned URL 생성
│       │   ├── S3PublicUrlAdapter.java          # S3 공개 URL 생성
│       │   └── S3ObjectMetadataAdapter.java
│       ├── S3ImageStorageAdapter.java
│       └── other/
│           └── DefaultImageAdapter.java
├── api/
│   └── dto/
│       ├── request/IssuePresignedUrlRequestDto.java
│       └── response/IssuePresignedUrlResponseDto.java
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   ├── IssuePresignedUrlUseCase.java
│   │   │   ├── ActivateImageUseCase.java
│   │   │   ├── ChangeImageUseCase.java
│   │   │   ├── DeleteImageUseCase.java
│   │   │   └── GetImageUrlByReferenceUseCase.java
│   │   └── out/
│   │       ├── ImagePort.java
│   │       ├── ImageRefPort.java
│   │       ├── PresignedUrlPort.java
│   │       └── PublicUrlPort.java
│   └── service/
│       ├── IssuePresignedUrlService.java        # Presigned URL 발급 로직
│       ├── ActivateImageService.java            # 이미지 활성화 로직
│       ├── ChangeImageService.java              # 이미지 교체 로직
│       ├── DeleteImageService.java              # 이미지 삭제 로직
│       └── GetImageUrlByReferenceService.java   # URL 조회 로직
├── domain/
│   ├── model/
│   │   ├── image/
│   │   │   ├── Image.java                      # 이미지 엔티티 (images 테이블)
│   │   │   └── ImageMeta.java
│   │   └── reference/
│   │       ├── ImageRef.java                   # 이미지 참조 엔티티 (image_reference 테이블)
│   │       ├── Reference.java                  # Embedded: (type, id)
│   │       └── ReferenceType.java              # enum: USER, GROUP, CARD_SET
│   └── policy/
│       └── ImageNamingPolicy.java              # S3 키/해시/콘텐츠타입 추출 정책
└── infrastructure/
    ├── config/
    │   ├── S3Config.java
    │   ├── AuditingConfig.java
    │   └── QuerydslConfig.java
    └── persistence/
        ├── jpa/
        │   ├── ImageRepository.java
        │   └── ImageRefRepository.java
        └── querydsl/
            ├── ImageRepositoryImpl.java
            └── ImageRefRepositoryImpl.java
```

---

## 핵심 클래스 및 역할

| 클래스 | 역할 |
|---|---|
| `ImageController` | HTTP POST /v1/images/upload 처리 |
| `ImageCommandGrpcService` | gRPC ImageCommandService 구현 (5개 메서드) |
| `IssuePresignedUrlService` | Presigned URL 발급, MD5 해시 기반 중복 제거 |
| `ActivateImageService` | imageRef → 엔티티 연결 |
| `ChangeImageService` | 기존 참조 교체 |
| `ImageNamingPolicy` | 파일명에서 hash/s3Key/contentType 추출 |
| `Image` | S3 저장 이미지 엔티티 (hash, s3Key, mimeType, sizeBytes) |
| `ImageRef` | 이미지-엔티티 연결 레코드; 활성화/비활성화/교체 상태 관리 |

---

## 데이터베이스 주요 테이블

### `images`

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT PK | |
| hash | VARCHAR(32) UNIQUE | MD5 해시 (중복 제거 키) |
| s3Key | VARCHAR(1024) | S3 오브젝트 키 (예: `image/abc123.jpg`) |
| mimeType | VARCHAR | MIME 타입 |
| sizeBytes | BIGINT | 파일 크기 |

### `image_reference`

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT PK | imageRefId |
| image_id | BIGINT FK → images.id | |
| reference_type | enum(USER\|GROUP\|CARD_SET) | 연결된 엔티티 유형 |
| reference_id | BIGINT | 연결된 엔티티 ID |
| version | BIGINT | 낙관적 락 |
| deleted_at | DATETIME | 논리 삭제 타임스탬프 |

---

## 이미지 업로드 흐름

```
Client
  │
  ├─ POST /v1/images/upload (fileName, type)
  │         │
  │    [hash 추출] → images 테이블에 동일 hash 존재?
  │         ├─ YES → imageRef 생성 + 공개 URL 반환
  │         └─ NO  → S3 Presigned URL 발급 + imageRef 생성 + Presigned URL 반환
  │
  ├─ PUT {presignedUrl} (이미지 파일) → S3 직접 업로드
  │
  └─ (호출 서비스가) gRPC ActivateImage(imageRefId, type, entityId)
             │
        imageRef.reference 설정 → 공개 URL 반환
```

---

## 외부 의존성

### gRPC — 노출하는 서비스

| 메서드 | 호출 주체 | 설명 |
|---|---|---|
| `GetUrlByReference` | User, Group, Cardset | 엔티티 유형+ID로 이미지 URL 조회 |
| `GetUrlsByIds` | Cardset | imageRefId 목록 → URL 맵 일괄 조회 |
| `ActivateImage` | User, Group, Cardset | 업로드 완료 후 imageRef 활성화 |
| `ChangeImage` | User, Group, Cardset | 이미지 교체 |
| `DeleteById` | User, Group, Cardset | 엔티티 삭제 시 이미지 참조 삭제 |

### AWS S3

- Presigned URL 생성 (업로드용, 유효시간 5분)
- 공개 URL 생성 (조회용)
- S3 키 형식: `image/{32자리 MD5 해시}.{확장자}`

---

## 환경 변수

| 변수 | 설명 |
|---|---|
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | MySQL 사용자명 |
| `SPRING_DATASOURCE_PASSWORD` | MySQL 비밀번호 |
| `AWS_S3_BUCKET` | S3 버킷명 |
| `AWS_S3_REGION` | AWS 리전 |
| `AWS_ACCESS_KEY_ID` | AWS 액세스 키 |
| `AWS_SECRET_ACCESS_KEY` | AWS 시크릿 키 |
| `GRPC_SERVER_PORT` | gRPC 서버 포트 (기본: 9092) |
