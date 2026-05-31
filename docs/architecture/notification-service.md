# Notification Service — Architecture

## 서비스 목적

Firebase FCM을 통한 푸시 알림 발송과 알림 내역 저장·조회를 담당한다.  
알림은 직접 HTTP로 생성하지 않고, Group 서비스가 RabbitMQ로 발행한 이벤트를 소비하여 생성한다.

- **HTTP 포트:** 8086
- **gRPC 포트:** 없음 (다른 서비스로부터 gRPC 호출을 받지 않음)

---

## 디렉토리 구조

```
src/main/java/flipnote/notification/
├── FlipNoteNotificationApplication.java
├── application/
│   ├── FcmSender.java                     # FCM 발송 포트 인터페이스
│   ├── FcmTokenService.java               # FCM 토큰 등록 유스케이스
│   ├── NotificationCommandService.java    # 알림 읽음 처리 유스케이스
│   ├── NotificationQueryService.java      # 알림 목록 조회 유스케이스
│   └── dto/
│       ├── command/NotificationListCommand.java
│       └── result/
│           ├── FcmSendResult.java
│           ├── NotificationResult.java
│           └── PagedResult.java
├── domain/
│   ├── common/
│   │   ├── BaseEntity.java
│   │   ├── BizException.java
│   │   ├── CommonErrorCode.java
│   │   └── ErrorCode.java
│   ├── fcmtoken/
│   │   ├── FcmToken.java                  # FCM 토큰 엔티티
│   │   └── FcmTokenRepository.java
│   └── notification/
│       ├── Notification.java              # 알림 엔티티
│       ├── NotificationErrorCode.java     # 도메인 에러 코드
│       ├── NotificationRepository.java
│       └── NotificationType.java          # GROUP_INVITE, GROUP_JOIN_REQUEST
├── infrastructure/
│   ├── config/
│   │   ├── AppConfig.java
│   │   ├── AsyncConfig.java               # 비동기 FCM 발송 스레드풀
│   │   ├── AsyncProperties.java
│   │   └── SwaggerConfig.java
│   ├── fcm/
│   │   ├── FcmErrorCode.java
│   │   ├── FirebaseConfig.java            # Firebase Admin SDK 초기화
│   │   └── FirebaseFcmSender.java         # FcmSender 구현체
│   ├── messaging/
│   │   ├── RabbitMQConfig.java            # Exchange / Queue / Binding 정의
│   │   ├── GroupInviteMessage.java        # 그룹 초대 이벤트 DTO
│   │   ├── GroupInviteMessageListener.java
│   │   ├── GroupJoinRequestMessage.java   # 그룹 가입 신청 이벤트 DTO
│   │   └── GroupJoinRequestMessageListener.java
│   └── persistence/
│       ├── JpaAuditingConfig.java
│       └── MapToJsonConverter.java        # Map<String,Object> ↔ JSON 컬럼 변환
└── interfaces/
    └── http/
        ├── NotificationController.java
        ├── NotificationControllerDocs.java  # Swagger 인터페이스
        ├── common/
        │   ├── ApiResponse.java
        │   ├── ApiResponseAdvice.java       # 응답 래핑 AOP
        │   ├── CursorPagingRequest.java
        │   ├── CursorPagingResponse.java
        │   ├── GlobalExceptionHandler.java
        │   └── HttpHeaders.java            # X-User-Id 상수
        └── dto/request/
            ├── NotificationListRequest.java
            └── TokenRegisterRequest.java
```

---

## 핵심 클래스 및 역할

| 클래스 | 역할 |
|--------|------|
| `NotificationQueryService` | 커서 기반 알림 목록 조회. `variables`를 `messages.properties` 키로 메시지 렌더링 |
| `NotificationCommandService` | 개별/전체 읽음 처리 |
| `FcmTokenService` | FCM 토큰 upsert (중복 토큰 안전하게 처리) |
| `FirebaseFcmSender` | Firebase Admin SDK로 멀티캐스트 메시지 발송, 비동기 실행 |
| `GroupInviteMessageListener` | RabbitMQ 소비 → GROUP_INVITE 알림 생성 + FCM 발송 |
| `GroupJoinRequestMessageListener` | RabbitMQ 소비 → GROUP_JOIN_REQUEST 알림 생성 + FCM 발송 |

---

## 데이터베이스

**MySQL** 사용

### `notifications` 테이블

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | BIGINT PK | 알림 ID |
| `receiver_id` | BIGINT | 수신자 유저 ID |
| `group_id` | BIGINT | 관련 그룹 ID (nullable) |
| `type` | VARCHAR(20) | `GROUP_INVITE` \| `GROUP_JOIN_REQUEST` |
| `variables` | JSON | 메시지 렌더링용 템플릿 변수 |
| `metadata` | JSON | 클라이언트 전달용 추가 데이터 |
| `is_read` | BOOLEAN | 읽음 여부 |
| `read_at` | DATETIME | 읽은 시각 (nullable) |
| `created_at` | DATETIME | 생성 시각 (BaseEntity) |
| `updated_at` | DATETIME | 수정 시각 (BaseEntity) |

### `fcm_token` 테이블 (FcmToken 엔티티)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | BIGINT PK | 토큰 레코드 ID |
| `user_id` | BIGINT | 소유 유저 ID |
| `token` | VARCHAR | FCM 디바이스 토큰 |

---

## 외부 의존성

### RabbitMQ — 소비 (Subscribe)

| Exchange | Queue | Routing Key | 발행 서비스 | 처리 |
|----------|-------|-------------|------------|------|
| `flipnote.notification` | `notification.group-invite.queue` | `notification.group.invite` | Group | GROUP_INVITE 알림 생성 + FCM 발송 |
| `flipnote.notification` | `notification.group-join-request.queue` | `notification.group.join-request` | Group | GROUP_JOIN_REQUEST 알림 생성 + FCM 발송 |

- DLX: `flipnote.notification.dlx`, DLQ: `flipnote.notification.dlq`
- 소비 실패 시 DLQ로 라우팅

### Firebase FCM

- Firebase Admin SDK를 사용해 멀티캐스트 푸시 발송
- 발송은 비동기(`@Async`) 스레드풀에서 실행
- FCM 발송 결과는 `FcmSendResult`로 반환 (성공/실패 토큰 분리)

### gRPC

- 이 서비스는 gRPC 서버를 노출하지 않음
- 다른 서비스로부터 gRPC 호출을 받지 않음

---

## 환경 변수

| 변수 | 설명 |
|------|------|
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자명 |
| `SPRING_DATASOURCE_PASSWORD` | DB 패스워드 |
| `SPRING_RABBITMQ_HOST` | RabbitMQ 호스트 |
| `SPRING_RABBITMQ_PORT` | RabbitMQ 포트 |
| `SPRING_RABBITMQ_USERNAME` | RabbitMQ 사용자명 |
| `SPRING_RABBITMQ_PASSWORD` | RabbitMQ 패스워드 |
| `FIREBASE_SERVICE_ACCOUNT_KEY` | Firebase Admin SDK 서비스 계정 JSON (Vault 관리) |
