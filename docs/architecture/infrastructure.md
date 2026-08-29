# FlipNote 인프라스트럭처

> 대상 독자: 프론트엔드 개발자 / 인프라 입문자
> 도커는 알지만 쿠버네티스는 개념만 아는 분을 기준으로 씁니다.

---

## 목차

1. [한 줄 요약](#1-한-줄-요약)
2. [전체 흐름도](#2-전체-흐름도)
3. [코드가 배포되는 과정 (CI/CD)](#3-코드가-배포되는-과정-cicd)
4. [쿠버네티스 — 서비스가 실제로 돌아가는 곳](#4-쿠버네티스--서비스가-실제로-돌아가는-곳)
5. [ArgoCD — 배포 자동화](#5-argocd--배포-자동화)
6. [시크릿 관리 (Vault + External Secrets)](#6-시크릿-관리-vault--external-secrets)
7. [공유 인프라 컴포넌트](#7-공유-인프라-컴포넌트)
8. [서비스별 환경변수 목록](#8-서비스별-환경변수-목록)
9. [도메인 및 네트워크 구조](#9-도메인-및-네트워크-구조)
10. [로컬 개발 시 차이점](#10-로컬-개발-시-차이점)

---

## 1. 한 줄 요약

> 코드를 `main` 브랜치에 push하면 → GitHub Actions가 Docker 이미지를 빌드해 GHCR에 올리고 → ArgoCD가 자동으로 감지해 Kubernetes 클러스터에 배포한다. 모든 비밀 값(DB 비밀번호, API 키 등)은 HashiCorp Vault에 저장되어 있고, 서비스가 뜰 때 자동으로 주입된다.

---

## 2. 전체 흐름도

```
개발자 로컬
  │
  │  git push → main
  ▼
GitHub (소스 코드 저장소)
  │
  ├─ [CI] GitHub Actions
  │    └─ 빌드 + 테스트 + 의존성 취약점 검사
  │
  └─ [CD] GitHub Actions
       └─ Docker 이미지 빌드 & GHCR에 Push
            │
            ▼
       GHCR (이미지 저장소)
       ghcr.io/dungbik/flipnote-{서비스}:main
            │
            │  새 이미지 감지 (ArgoCD Image Updater)
            ▼
       ArgoCD (자동 배포 도구)
       FlipNote-Infra 레포를 바라보며 Helm Chart 기반으로 배포
            │
            ▼
       Kubernetes 클러스터 (namespace: flipnote)
            │
            ├─ API Gateway Pod          :8080
            ├─ User Service Pod         :8081 / gRPC :9091
            ├─ Image Service Pod        :8082 / gRPC :9092
            ├─ Reaction Service Pod     :8083 / gRPC :9093
            ├─ Group Service Pod        :8084 / gRPC :9094
            ├─ Cardset Service Pod      :8085 / gRPC :9095
            └─ Notification Service Pod :8086
                 │
                 └─ Nginx Ingress → api3.flipnote.site (외부 유일한 진입점)
```

---

## 3. 코드가 배포되는 과정 (CI/CD)

### 3-1. CI (Continuous Integration) — 코드 검증

`main` 브랜치에 push 하거나 PR을 열면 자동으로 실행된다.

```
main push / PR open
  │
  ├─ [Job 1] build
  │    ① 코드 체크아웃
  │    ② JDK 21 (Java 서비스) 또는 Node 22 (Cardset) 설치
  │    ③ 빌드 (./gradlew build -x test)
  │    ④ 테스트 (./gradlew test)
  │
  └─ [Job 2] dependency-check
       ① OWASP Dependency-Check로 라이브러리 취약점 스캔
       ② HTML 보고서를 GitHub Artifact로 업로드
```

### 3-2. CD (Continuous Delivery) — 이미지 빌드 & 배포

    GHCR이란
    GHCR = GitHub Container Registry .
    GitHub에서 운영하는 Docker 이미지 저장소예요.

    Docker Hub랑 똑같은 개념인데, GitHub 안에 붙어있는 버전이라고 보면 돼요.

    Docker Hub:  docker.io/myapp:latest
    GHCR:       ghcr.io/dungbik/flipnote-user:main

    FlipNote는 각 서비스의 Docker 이미지를 GHCR에 올려두고, ArgoCD가 거기서
    이미지를 가져와서 쿠버네티스에 배포하는 구조예요.

    GitHub 레포랑 같은 계정/조직으로 묶여있어서 권한 관리가 편하다는 장점이
    있어요.

`main` 브랜치 push 시에만 실행된다.

```
main push
  │
  ① GHCR(GitHub Container Registry)에 로그인
  ② Docker 이미지 빌드
       - Java 서비스: gradle bootJar → eclipse-temurin:21-jre 위에서 실행
       - Cardset: npm build → node:22-alpine 위에서 실행
  ③ GHCR에 이미지 Push
       ghcr.io/dungbik/flipnote-user:main  ← 태그가 브랜치 이름
       ghcr.io/dungbik/flipnote-group:main
       ... (서비스마다 각자 레포에 CD 워크플로 있음)
  │
  [여기서 GitHub Actions 역할 끝]
  │
  ④ ArgoCD Image Updater가 GHCR을 주기적으로 폴링
       새 이미지 digest 감지 → FlipNote-Infra 레포의 values.yaml에
       이미지 태그를 자동 커밋
  ⑤ ArgoCD가 FlipNote-Infra 레포 변경 감지 → 클러스터 자동 동기화
```

> **핵심**: 코드 레포(FlipNote-User 등)와 인프라 레포(FlipNote-Infra)가 **분리**되어 있다.
> 코드 레포는 이미지를 만들기만 하고, 실제 배포 설정은 인프라 레포에서 관리한다.
> 이 패턴을 **GitOps**라고 한다.

---

## 4. 쿠버네티스 — 서비스가 실제로 돌아가는 곳

### 도커와 쿠버네티스 비교

| 개념               | 도커 비유                       | 쿠버네티스 실체                   |
| ------------------ | ------------------------------- | --------------------------------- |
| 컨테이너           | `docker run` 으로 띄운 프로세스 | Pod 안의 컨테이너                 |
| 서비스 묶음        | `docker-compose.yml`            | Deployment                        |
| 내부 네트워크 주소 | 컨테이너 이름 (예: `mysql`)     | Service 이름 (예: `user-service`) |
| 외부 접근 설정     | `-p 8080:8080` 포트 매핑        | Ingress                           |
| 환경변수 파일      | `.env` 파일                     | Secret / ConfigMap                |

### FlipNote 클러스터 구성

```
Namespace: flipnote
│
├─ Deployment: api-gateway          → Pod 1개
├─ Deployment: user-service         → Pod 1개
├─ Deployment: image-service        → Pod 1개
├─ Deployment: reaction-service     → Pod 1개
├─ Deployment: group-service        → Pod 1개
├─ Deployment: cardset-service      → Pod 1개
├─ Deployment: notification-service → Pod 1개
│
├─ Service: (각 Deployment마다 1개)
│    └─ type: ClusterIP  ← 클러스터 내부에서만 접근 가능
│         서비스끼리는 "http://user-service:8081" 처럼 이름으로 통신
│
└─ Ingress (Nginx)
     └─ api3.flipnote.site → api-gateway:8080
```

### Helm Chart — 배포 설정을 관리하는 방식

쿠버네티스에 직접 YAML을 여러 개 쓰는 대신 **Helm Chart**라는 템플릿 시스템을 사용한다.

```
FlipNote-Infra/charts/apps/{서비스}/
├─ Chart.yaml       # 차트 이름, 버전
├─ values.yaml      # 실제 설정값 (이미지 태그, 포트, 환경변수 등)
└─ templates/       # 쿠버네티스 YAML 템플릿
```

`values.yaml`이 핵심 설정 파일이다. 여기서 이미지 버전, 리소스 제한, 외부 시크릿 설정을 한다.

---

## 5. ArgoCD — 배포 자동화

ArgoCD는 **FlipNote-Infra 레포를 계속 감시하다가** 변경이 생기면 자동으로 클러스터에 적용한다.

```
FlipNote-Infra 레포 (GitHub)
  │  변경 감지
  ▼
ArgoCD
  ├─ syncPolicy: automated       ← 사람이 승인 없이 자동 배포
  ├─ prune: true                 ← 레포에서 삭제된 리소스는 클러스터에서도 삭제
  └─ selfHeal: true              ← 누군가 클러스터를 직접 수정해도 레포 기준으로 복원
```

### ArgoCD Image Updater

이미지가 새로 올라오면 자동으로 배포까지 이어지는 장치.

```
GHCR에 새 이미지 Push
  │
  ArgoCD Image Updater가 감지
  │
  FlipNote-Infra 레포의 values.yaml에 새 digest 자동 커밋
  │
  ArgoCD가 커밋 감지 → 클러스터 자동 동기화
  │
  새 Pod 생성 → 구 Pod 교체 (무중단)
```

> **결과**: 개발자는 코드만 push하면 배포까지 자동으로 된다.

---

## 6. 시크릿 관리 (Vault + External Secrets)

### 문제

DB 비밀번호, JWT 시크릿 키, AWS 키 등을 코드나 Git에 넣으면 보안상 위험하다.

### 해결 구조

```
HashiCorp Vault (vault.flipnote.site:8200)
  └─ 모든 시크릿을 중앙에서 안전하게 보관
       예) secret/data/flipnote/user-service
           ├─ DB_URL: jdbc:mysql://...
           ├─ JWT_SECRET: xxxxxxxx
           └─ APP_RESEND_API_KEY: re_xxxxxxx

       secret/data/flipnote/cardset-service
           ├─ DB_HOST: ...
           ├─ RABBITMQ_URL: ...
           └─ ...

External Secrets Operator (쿠버네티스 안에서 실행)
  └─ Vault에서 값을 가져와 K8s Secret 오브젝트로 변환
       refreshInterval: 1h  ← 1시간마다 Vault에서 최신값으로 갱신

K8s Secret
  └─ Pod의 환경변수로 자동 주입
       예) user-service Pod
           DB_URL=jdbc:mysql://...
           JWT_SECRET=xxxxxxxx
```

### 흐름 요약

```
Vault에 시크릿 저장
  ↓
External Secrets Operator가 1시간마다 Vault 조회
  ↓
K8s Secret 생성/갱신
  ↓
Pod가 뜰 때 환경변수로 자동 주입
```

> **왜 이렇게 하나?** 시크릿을 Git에 커밋하지 않아도 되고, 값이 바뀌어도 코드 배포 없이 적용된다.

---

## 7. 공유 인프라 컴포넌트

서비스들이 함께 사용하는 외부 시스템들.

### MySQL

각 서비스가 **독립된 DB 스키마**를 사용한다. 같은 MySQL 서버 안에 있어도 서로 다른 DB다.

| 서비스       | DB 이름               |
| ------------ | --------------------- |
| User         | flipnote_user         |
| Image        | flipnote_image        |
| Reaction     | flipnote_reaction     |
| Group        | flipnote_group        |
| Cardset      | flipnote_cardset      |
| Notification | flipnote_notification |

> 서비스 A가 서비스 B의 DB에 직접 접근하는 일은 없다. 반드시 API나 gRPC를 통해서만 통신한다.

### Redis

| 서비스  | 용도                                               |
| ------- | -------------------------------------------------- |
| User    | JWT 토큰 블랙리스트, 이메일 인증 코드, 세션 무효화 |
| Image   | (Redis 사용, 세부 용도는 서비스 내부)              |
| Cardset | WebSocket 세션 관리                                |

### RabbitMQ

서비스 간 **비동기 메시지 전달**에 사용. 발신자는 메시지를 던지고 끝, 수신자는 나중에 처리한다.

```
Group ──[group.invite.exchange]──────→ Notification
       그룹 초대 이벤트                  FCM 푸시 발송

Group ──[group.join.request.exchange]─→ Notification
       그룹 가입 요청 이벤트              FCM 푸시 발송

Reaction ──[reaction.exchange]────────→ Cardset
           좋아요/북마크 이벤트            반응 카운트 동기화
```

### AWS S3

Image 서비스만 사용한다.

- 버킷: `flipnote-bucket` (ap-northeast-2, 서울 리전)
- 사용자가 이미지 업로드 시 S3에 직접 업로드할 수 있도록 Presigned URL을 발급한다.
- 기본 그룹 이미지 URL: `https://flipnote-bucket.s3.ap-northeast-2.amazonaws.com/image/default/group.png`

### Firebase FCM

Notification 서비스만 사용한다. 모바일/웹 푸시 알림 발송에 사용.

- 서비스 계정 JSON(`FIREBASE_SERVICE_ACCOUNT_JSON`)을 환경변수로 주입받아 초기화한다.

---

## 8. 서비스별 환경변수 목록

Vault 경로: `secret/data/flipnote/{서비스이름}`

### User Service

| 환경변수               | 설명                        | 기본값                                      |
| ---------------------- | --------------------------- | ------------------------------------------- |
| `DB_URL`               | MySQL 연결 URL              | `jdbc:mysql://localhost:3306/flipnote_user` |
| `DB_USERNAME`          | DB 사용자                   | `root`                                      |
| `DB_PASSWORD`          | DB 비밀번호                 | `root`                                      |
| `REDIS_HOST`           | Redis 호스트                | `localhost`                                 |
| `REDIS_PORT`           | Redis 포트                  | `6379`                                      |
| `REDIS_PASSWORD`       | Redis 비밀번호              | (없음)                                      |
| `JWT_SECRET`           | JWT 서명 키                 | **없음 (필수)**                             |
| `GOOGLE_CLIENT_ID`     | OAuth2 구글 클라이언트 ID   | (없음)                                      |
| `GOOGLE_CLIENT_SECRET` | OAuth2 구글 시크릿          | (없음)                                      |
| `APP_RESEND_API_KEY`   | 이메일 발송 API 키 (Resend) | **없음 (필수)**                             |
| `APP_CLIENT_URL`       | 프론트엔드 URL              | `http://localhost:3000`                     |

### Image Service

| 환경변수                     | 설명           |
| ---------------------------- | -------------- |
| `DB_URL`                     | MySQL 연결 URL |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자      |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호    |
| `S3_ACCESS_KEY`              | AWS Access Key |
| `S3_SECRET_KEY`              | AWS Secret Key |
| `S3_BUCKET_NAME`             | S3 버킷 이름   |
| `S3_BUCKET_REGION`           | S3 리전        |
| `SPRING_DATA_REDIS_HOST`     | Redis 호스트   |
| `SPRING_DATA_REDIS_PORT`     | Redis 포트     |
| `SPRING_DATA_REDIS_PASSWORD` | Redis 비밀번호 |

### Reaction Service

| 환경변수            | 설명              | 기본값      |
| ------------------- | ----------------- | ----------- |
| `DB_URL`            | MySQL 연결 URL    | 없음 (필수) |
| `DB_USERNAME`       | DB 사용자         | 없음 (필수) |
| `DB_PASSWORD`       | DB 비밀번호       | 없음 (필수) |
| `RABBITMQ_HOST`     | RabbitMQ 호스트   | `localhost` |
| `RABBITMQ_PORT`     | RabbitMQ 포트     | `5672`      |
| `RABBITMQ_USERNAME` | RabbitMQ 사용자   | `guest`     |
| `RABBITMQ_PASSWORD` | RabbitMQ 비밀번호 | (없음)      |

### Group Service

| 환경변수                     | 설명                   | 기본값           |
| ---------------------------- | ---------------------- | ---------------- |
| `SPRING_DATASOURCE_URL`      | MySQL 연결 URL         | 없음 (필수)      |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자              | 없음 (필수)      |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호            | 없음 (필수)      |
| `GRPC_IMAGE_URL`             | Image 서비스 gRPC 주소 | `localhost:9092` |
| `GRPC_USER_URL`              | User 서비스 gRPC 주소  | `localhost:9091` |
| `RABBITMQ_HOST`              | RabbitMQ 호스트        | `localhost`      |
| `RABBITMQ_PORT`              | RabbitMQ 포트          | `5672`           |
| `RABBITMQ_USERNAME`          | RabbitMQ 사용자        | `guest`          |
| `RABBITMQ_PASSWORD`          | RabbitMQ 비밀번호      | (없음)           |
| `RESEND_API_KEY`             | 이메일 발송 API 키     | 없음 (필수)      |

### Cardset Service

| 환경변수                                       | 설명                                        |
| ---------------------------------------------- | ------------------------------------------- |
| `DB_HOST` / `DB_PORT` / `DB_DATABASE`          | MySQL 연결 정보                             |
| `DB_USERNAME` / `DB_PASSWORD`                  | DB 인증                                     |
| `DB_SYNCHRONIZE`                               | TypeORM 스키마 자동 동기화 (`true`/`false`) |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 연결 정보                             |
| `RABBITMQ_URL`                                 | RabbitMQ 연결 URL (`amqp://...`)            |
| `USER_GRPC_URL`                                | User 서비스 gRPC 주소                       |
| `GROUP_GRPC_URL`                               | Group 서비스 gRPC 주소                      |
| `IMAGE_GRPC_URL`                               | Image 서비스 gRPC 주소                      |
| `GRPC_REACTION_URL`                            | Reaction 서비스 gRPC 주소                   |
| `GRPC_PORT`                                    | 이 서비스의 gRPC 포트 (9095)                |
| `PORT`                                         | HTTP 포트 (8085)                            |
| `NODE_ENV`                                     | 환경 (`production` / `development`)         |
| `DEFAULT_CARDSET_IMAGE_URL`                    | 기본 카드셋 이미지 URL                      |
| `SKIP_WS_AUTH`                                 | WebSocket 인증 스킵 여부 (개발용)           |
| `SKIP_USER_GRPC`                               | User gRPC 호출 스킵 여부 (개발용)           |
| `SKIP_REACTION_GRPC`                           | Reaction gRPC 호출 스킵 여부 (개발용)       |

### Notification Service

| 환경변수                                                                      | 설명                                      |
| ----------------------------------------------------------------------------- | ----------------------------------------- |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`                                      | MySQL 연결 정보                           |
| `JPA_DDL_AUTO`                                                                | JPA 스키마 전략 (`update`, `validate` 등) |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | RabbitMQ 연결 정보                        |
| `FIREBASE_SERVICE_ACCOUNT_JSON`                                               | Firebase 서비스 계정 JSON (전체 내용)     |
| `SPRINGDOC_SERVER_URL`                                                        | Swagger 문서에 표시될 서버 URL            |

---

## 9. 도메인 및 네트워크 구조

### 외부 접근 (인터넷)

```
사용자/프론트엔드
  │
  HTTPS → api3.flipnote.site
  │
  Nginx Ingress (쿠버네티스 클러스터 진입점)
  │
  API Gateway (8080)
  │  ├─ JWT 검증
  │  ├─ 라우팅
  │  └─ X-User-Id 헤더 주입
  ▼
각 마이크로서비스 (클러스터 내부, 외부 직접 접근 불가)
```

### 클러스터 내부 통신

```
서비스끼리는 쿠버네티스 Service 이름으로 통신한다.

HTTP:   http://user-service:8081
HTTP:   http://cardset-service:8085
gRPC:   user-service:9091
gRPC:   image-service:9092
gRPC:   reaction-service:9093
gRPC:   group-service:9094
gRPC:   cardset-service:9095
```

### 기타 외부 도메인

| 서비스                | 주소                         |
| --------------------- | ---------------------------- |
| Vault (시크릿 저장소) | `vault.flipnote.site:8200`   |
| 프론트엔드            | `flipnote.site`              |
| GHCR (이미지 저장소)  | `ghcr.io/dungbik/flipnote-*` |

---

## 10. 로컬 개발 시 차이점

| 항목              | 운영 (쿠버네티스)                             | 로컬                                     |
| ----------------- | --------------------------------------------- | ---------------------------------------- |
| 서비스 URL        | `http://user-service:8081`                    | `http://localhost:8081`                  |
| 시크릿            | Vault → External Secrets → 환경변수 자동 주입 | 직접 환경변수 설정 필요                  |
| 인프라 (MySQL 등) | 클러스터 내부 또는 외부 DB 서버               | Docker로 직접 띄움                       |
| Gateway 라우팅    | `application.yml` (k8s 내부 주소)             | `application-local.yml` (localhost 주소) |
| 이미지 자동 배포  | ArgoCD Image Updater                          | 해당 없음                                |

### Gateway 로컬 실행 시 주의

Gateway에는 `application-local.yml`이 있어서 `SPRING_PROFILES_ACTIVE=local`로 실행하면 서비스 주소가 자동으로 localhost로 바뀐다. 단, Cardset/Notification 경로는 이 파일에 아직 반영이 안 돼 있어 수동 확인 필요.

### 로컬 실행 최소 요구 조건

서비스를 1개라도 로컬에서 실행하려면 아래가 필요하다.

1. **MySQL** — Docker로 띄우거나 로컬 설치
2. **Redis** — User, Image, Cardset 서비스에 필요
3. **RabbitMQ** — Group, Reaction, Cardset, Notification에 필요
4. **환경변수** — Vault에서 직접 가져오거나 백엔드 팀원에게 받아야 함
   - 특히 `JWT_SECRET`, `APP_RESEND_API_KEY`, DB 접속 정보 등 기본값이 없는 것들
