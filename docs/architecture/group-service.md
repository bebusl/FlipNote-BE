# Group Service 아키텍처

## 서비스 목적

그룹 생성·수정·삭제, 멤버 관리, 초대, 가입 신청, 역할·권한 관리를 담당한다.  
다른 서비스에서 그룹 소속 여부와 이름을 조회할 수 있도록 gRPC 인터페이스를 노출한다.

---

## 포트 정보

| 프로토콜 | 포트 |
|----------|------|
| HTTP | 8084 |
| gRPC | 9094 |

---

## 디렉토리 구조

```
src/main/java/flipnote/group/
├── adapter/
│   ├── in/
│   │   ├── grpc/
│   │   │   └── GroupCommandService.java       # gRPC 서버 구현체
│   │   └── web/
│   │       ├── GroupController.java           # 그룹 CRUD, 목록
│   │       ├── InviteController.java          # 초대 발송/응답/취소
│   │       ├── JoinController.java            # 가입 신청 CRUD
│   │       ├── MeController.java              # 내 가입신청/초대 조회
│   │       ├── MemberController.java          # 멤버 조회/강퇴/역할변경
│   │       └── PermissionController.java      # 권한 부여/회수/조회
│   └── out/
│       ├── entity/                            # JPA 엔티티
│       └── persistence/                       # Repository 어댑터
├── api/
│   ├── advice/GlobalExceptionHandler.java     # 공통 에러 핸들러
│   └── dto/
│       ├── request/                           # 요청 DTO
│       └── response/                          # 응답 DTO
├── application/
│   ├── dto/                                   # RabbitMQ 메시지 DTO
│   ├── listener/                              # 이벤트 리스너
│   ├── port/
│   │   ├── in/                                # UseCase 인터페이스 + Command/Result
│   │   └── out/                               # Repository 포트 인터페이스
│   └── service/                               # UseCase 구현체 (서비스 클래스)
├── domain/
│   ├── event/                                 # 도메인 이벤트
│   ├── model/                                 # 도메인 모델 (enum, value object)
│   └── policy/
│       ├── BusinessException.java
│       └── ErrorCode.java
├── global/
│   └── config/                                # 설정 클래스
└── infrastructure/
    ├── email/                                 # Resend 이메일 서비스
    ├── messaging/                             # RabbitMQ 메시지 프로듀서
    └── persistence/
        ├── jpa/                               # Spring Data JPA 리포지토리
        └── querydsl/                          # QueryDSL 동적 쿼리
```

---

## 핵심 클래스/파일

| 클래스 | 역할 |
|--------|------|
| `GroupController` | 그룹 생성/수정/삭제/조회 HTTP 엔드포인트 |
| `InviteController` | 초대 발송, 취소, 응답, 목록 조회 |
| `JoinController` | 가입 신청 제출, 목록 조회, 수락/거절, 취소 |
| `MeController` | 내 가입 신청 목록, 내가 받은 초대 목록 |
| `MemberController` | 멤버 조회, 강퇴, 역할 변경 |
| `PermissionController` | 역할별 권한 부여/회수/조회 |
| `GroupCommandService` (gRPC) | 그룹명 조회, 멤버 여부 확인, 내 그룹 ID 목록 제공 |
| `GroupEntity` | 그룹 JPA 엔티티 (`app_groups` 테이블) |
| `GroupMemberEntity` | 멤버십 JPA 엔티티 |
| `InviteEntity` | 초대 JPA 엔티티 |
| `JoinEntity` | 가입 신청 JPA 엔티티 |
| `PermissionEntity` | 역할별 권한 JPA 엔티티 |
| `RoleEntity` | 역할 JPA 엔티티 |
| `GroupRepositoryImpl` | QueryDSL 기반 커서 페이징 조회 구현 |
| `GroupJoinRequestMessageProducer` | RabbitMQ 가입 요청 메시지 발행 |
| `GuestInviteEventListener` | 비가입 사용자 초대 이메일 발송 이벤트 처리 |
| `ResendEmailService` | Resend API를 통한 이메일 발송 |
| `ErrorCode` | 서비스 에러 코드 열거형 |

---

## 데이터베이스 주요 테이블/엔티티

| 테이블 | 엔티티 | 설명 |
|--------|--------|------|
| `app_groups` | `GroupEntity` | 그룹 기본 정보 (이름, 카테고리, joinPolicy, visibility, maxMember, memberCount) |
| `group_member` | `GroupMemberEntity` | 그룹-사용자 멤버십 관계, 역할 정보 |
| `invite` | `InviteEntity` | 초대 정보 (초대자, 수신자 이메일/userId, 상태) |
| `join_request` | `JoinEntity` | 가입 신청 정보 (신청자, 그룹, 자기소개, 상태) |
| `group_role_permission` | `PermissionEntity` | 역할별 권한 매핑 |
| `group_role` | `RoleEntity` | 그룹 역할 정의 |

---

## 외부 의존성

### gRPC 클라이언트 (아웃바운드)

| 대상 서비스 | 포트 | 사용 목적 |
|-------------|------|----------|
| User Service | 9091 | 멤버 닉네임/프로필 이미지 조회, 이메일로 userId 조회 |
| Image Service | 9092 | imageRefId로 이미지 URL 조회 |

### gRPC 서버 (인바운드, 포트 9094)

| 메서드 | 호출 서비스 | 설명 |
|--------|------------|------|
| `GetGroupName` | Cardset, Notification | 그룹 ID로 그룹 이름 조회 |
| `CheckUserInGroup` | Cardset | 사용자가 그룹 멤버인지 확인 |
| `GetMyGroup` | Cardset, Reaction | 사용자가 속한 그룹 ID 목록 조회 |

### RabbitMQ (메시지 발행)

| Exchange / Routing Key | 발행 조건 | 수신 서비스 |
|----------------------|----------|------------|
| 가입 신청 수락 메시지 | 가입 신청 수락 시 | Notification |

### 이메일 (Resend API)

비가입 사용자를 초대할 경우 Resend API를 통해 초대 이메일을 발송한다.  
이메일 템플릿: `src/main/resources/templates/email/guest-group-invitation.html`

---

## 환경 변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `GRPC_IMAGE_URL` | Image Service gRPC 주소 | `localhost:9092` |
| `GRPC_USER_URL` | User Service gRPC 주소 | `localhost:9091` |
| `RABBITMQ_HOST` | RabbitMQ 호스트 | `localhost` |
| `RABBITMQ_PORT` | RabbitMQ 포트 | `5672` |
| `RABBITMQ_USERNAME` | RabbitMQ 사용자명 | `guest` |
| `RABBITMQ_PASSWORD` | RabbitMQ 비밀번호 | (없음) |
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL | — |
| `SPRING_DATASOURCE_USERNAME` | MySQL 사용자명 | — |
| `SPRING_DATASOURCE_PASSWORD` | MySQL 비밀번호 | — |
| `RESEND_API_KEY` | Resend 이메일 API 키 | — |
| `RESEND_FROM_EMAIL` | 발신자 이메일 주소 | `FlipNote <no-reply@flipnote.site>` |
| `APP_CLIENT_URL` | 프론트엔드 클라이언트 URL | `https://flipnote.site` |

---

## 아키텍처 패턴

Hexagonal Architecture(포트&어댑터 패턴)를 적용한다.

- **인바운드 어댑터**: `adapter/in/web/` (HTTP), `adapter/in/grpc/` (gRPC)
- **아웃바운드 어댑터**: `adapter/out/persistence/` (DB), `infrastructure/messaging/` (RabbitMQ), `infrastructure/email/` (이메일)
- **포트**: `application/port/in/` (UseCase 인터페이스), `application/port/out/` (Repository 인터페이스)
- **서비스**: `application/service/` — 각 UseCase를 독립된 서비스 클래스로 구현
- **도메인 모델**: `domain/model/` — enum 및 값 객체로 도메인 규칙 표현

커서 기반 페이지네이션은 QueryDSL(`GroupRepositoryImpl`)로 구현하며, 초대 목록은 Spring Data Page 기반 오프셋 페이지네이션을 사용한다.
