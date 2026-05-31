# Image 서비스 API

Image 서비스는 AWS S3 Presigned URL 방식으로 이미지 업로드를 처리하고, gRPC를 통해 다른 서비스에 이미지 URL을 제공합니다.

**Base URL:** `http://localhost:8080` (API Gateway 경유)  
**내부 포트:** HTTP 8082 / gRPC 9092

---

## 이미지 업로드 흐름

이미지 업로드는 2단계로 이루어집니다.

```
1. POST /v1/images/upload  →  presignedUrl + imageRefId 수신
2. presignedUrl로 S3에 직접 PUT 업로드
3. (서버 측) ActivateImage gRPC 호출로 이미지 활성화
```

> **중복 제거**: 동일한 MD5 해시의 파일이 이미 S3에 존재하면 업로드 없이 기존 공개 URL을 즉시 반환합니다.

---

## HTTP API

### Presigned URL 발급

클라이언트가 S3에 직접 업로드하기 위한 서명된 URL을 발급받습니다.

**POST** `/v1/images/upload`  
인증 불필요

#### 요청

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| fileName | string | ✓ | 파일명 (32자리 MD5 해시 + 확장자 형식) |
| type | string | ✓ | 이미지가 연결될 엔티티 유형 |

**fileName 형식**: `{32자리 MD5 소문자 16진수}.{확장자}`  
허용 확장자: `jpg`, `jpeg`, `png`, `gif`  
예시: `d41d8cd98f00b204e9800998ecf8427e.png`

**type 허용값**: `USER` / `GROUP` / `CARD_SET`

```bash
curl -X POST http://localhost:8080/v1/images/upload \
  -H "Content-Type: application/json" \
  -d '{
    "fileName": "d41d8cd98f00b204e9800998ecf8427e.jpg",
    "type": "USER"
  }'
```

#### 응답 (200)

```json
{
  "url": "https://s3.ap-northeast-2.amazonaws.com/bucket/image/d41d8cd98f00b204e9800998ecf8427e.jpg?X-Amz-Signature=...",
  "imageRefId": 42
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| url | string | S3 Presigned URL (유효시간 5분) 또는 기존 공개 URL |
| imageRefId | number | 이미지 참조 ID — 업로드 후 서버에 전달 필요 |

> `url`은 두 가지 경우를 가집니다.
> - **신규 파일**: S3 PUT 업로드용 Presigned URL (5분 내 업로드 필요)
> - **기존 파일(중복)**: 이미 S3에 존재하는 공개 URL (별도 업로드 불필요)

#### S3 PUT 업로드 예시

```bash
curl -X PUT "{presignedUrl}" \
  -H "Content-Type: image/jpeg" \
  --upload-file ./my-image.jpg
```

#### 에러

| 상태 코드 | 설명 |
|-----------|------|
| 400 | fileName 형식이 올바르지 않음 (32자리 MD5 해시 + 허용 확장자 아님) |
| 400 | fileName 미입력 |

---

## gRPC API

gRPC는 서비스 간 내부 통신 전용이며 외부에서 직접 호출하지 않습니다.  
**패키지**: `image.v1` / **서비스**: `ImageCommandService` / **포트**: `9092`

### 참조로 이미지 URL 조회 — `GetUrlByReference`

특정 엔티티(유저, 그룹, 카드셋)에 연결된 이미지의 공개 URL을 조회합니다.

| 요청 필드 | 타입 | 설명 |
|-----------|------|------|
| reference_type | enum | `USER(1)`, `GROUP(2)`, `CARD_SET(3)` |
| reference_id | int64 | 엔티티 ID |

| 응답 필드 | 타입 | 설명 |
|-----------|------|------|
| image_url | string | 공개 S3 URL |

---

### 다수 imageRef URL 일괄 조회 — `GetUrlsByIds`

여러 imageRefId를 받아 ID → URL 맵으로 반환합니다.

| 요청 필드 | 타입 | 설명 |
|-----------|------|------|
| ids | repeated int64 | imageRefId 목록 |

| 응답 필드 | 타입 | 설명 |
|-----------|------|------|
| image_urls | map\<int64, string\> | imageRefId → 공개 S3 URL |

---

### 이미지 활성화 — `ActivateImage`

클라이언트의 S3 업로드가 완료된 후 호출합니다. imageRef를 실제 엔티티에 연결합니다.

| 요청 필드 | 타입 | 설명 |
|-----------|------|------|
| image_ref_id | int64 | POST /v1/images/upload에서 받은 imageRefId |
| reference_type | enum | `USER(1)`, `GROUP(2)`, `CARD_SET(3)` |
| reference_id | int64 | 연결할 엔티티 ID |

| 응답 필드 | 타입 | 설명 |
|-----------|------|------|
| url | string | 활성화된 이미지의 공개 S3 URL |

---

### 이미지 변경 — `ChangeImage`

기존 엔티티의 이미지를 새 이미지로 교체합니다.

| 요청 필드 | 타입 | 설명 |
|-----------|------|------|
| reference_type | enum | `USER(1)`, `GROUP(2)`, `CARD_SET(3)` |
| reference_id | int64 | 대상 엔티티 ID |
| image_ref_id | int64 | 새 imageRefId |

| 응답 필드 | 타입 | 설명 |
|-----------|------|------|
| image_ref_id | int64 | 업데이트된 imageRefId |
| url | string | 새 이미지의 공개 S3 URL |

---

### 이미지 삭제 — `DeleteById`

imageRef를 완전 비활성화합니다. 엔티티(유저, 그룹, 카드셋) 삭제 시 호출합니다.

| 요청 필드 | 타입 | 설명 |
|-----------|------|------|
| image_ref_id | int64 | 삭제할 imageRefId |

응답: 없음 (빈 메시지)

---

## 주요 개념

### ImageRef (이미지 참조)

Image 엔티티와 실제 엔티티(유저/그룹/카드셋) 사이의 연결 레코드입니다.

- **미활성 상태**: `POST /v1/images/upload` 직후, 아직 엔티티에 연결되지 않은 상태
- **활성 상태**: `ActivateImage` gRPC 호출 후, 특정 엔티티에 연결된 상태
- **삭제 상태**: `DeleteById` 후, 논리 삭제됨

### 파일명 규칙

업로드 전 클라이언트가 이미지 파일의 MD5 해시를 계산하여 `{hash}.{ext}` 형식으로 fileName을 구성해야 합니다.  
서버는 이 해시를 기반으로 중복 이미지 여부를 판별합니다.
