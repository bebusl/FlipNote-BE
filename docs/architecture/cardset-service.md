# Cardset 서비스 아키텍처

## 서비스 목적

카드셋(플래시카드 묶음)과 카드를 관리하고, Yjs CRDT 기반의 실시간 협업 편집을 제공한다. 그룹 내 멤버가 카드셋을 생성하고 여러 매니저가 동시에 편집할 수 있다.

- HTTP 포트: **8085**
- gRPC 포트: **9095**
- 스택: NestJS / TypeScript, MySQL, Redis, RabbitMQ, Socket.IO, Yjs

---

## 디렉토리 구조

```
FlipNote-Cardset/src/
├── app.module.ts
├── main.ts
│
├── auth/
│   ├── domain/
│   │   └── auth.service.ts               # JWT 검증 로직
│   └── infrastructure/guard/
│       └── ws-auth.guard.ts              # WebSocket JWT 가드
│
├── cardset/
│   ├── application/
│   │   ├── cardset.use-case.ts           # 카드셋 유즈케이스
│   │   ├── card.use-case.ts              # 카드 유즈케이스
│   │   └── dto/
│   │       ├── request/                  # 요청 DTO
│   │       │   ├── create-cardset.request.ts
│   │       │   ├── update-cardset.request.ts
│   │       │   ├── cardset-search.request.ts
│   │       │   ├── create-card.request.ts     (미사용)
│   │       │   ├── update-card.request.ts     (미사용)
│   │       │   └── reorder-cards.request.ts   (미사용)
│   │       └── response/                 # 응답 DTO
│   │           ├── cardset-create.response.ts
│   │           ├── cardset.response.ts
│   │           ├── cardset-list-item.response.ts
│   │           ├── cardset-page.response.ts
│   │           ├── card.response.ts           (미사용)
│   │           ├── card-create.response.ts    (미사용)
│   │           ├── yjs-card.response.ts
│   │           └── manager-info.response.ts
│   │
│   ├── domain/
│   │   ├── model/
│   │   │   ├── cardset.ts                # 카드셋 도메인 모델
│   │   │   ├── card.ts                   # 카드 도메인 모델
│   │   │   ├── cardset-manager.ts        # 매니저 관계 모델
│   │   │   └── visibility.ts             # Enum: PUBLIC | PRIVATE
│   │   ├── repository/                   # Repository 인터페이스
│   │   │   ├── cardset.repository.ts
│   │   │   ├── card.repository.ts
│   │   │   ├── cardset-manager.repository.ts
│   │   │   └── cardset-metadata.repository.ts
│   │   └── service/
│   │       └── cardset-card.domain-service.ts
│   │
│   └── infrastructure/
│       ├── http/                         # REST 컨트롤러
│       │   ├── cardset.controller.ts     # /v1/card-sets
│       │   ├── card.controller.ts        # /v1/cards (미사용)
│       │   └── group-cardset.controller.ts # /v1/groups/:id/card-sets
│       ├── grpc/
│       │   ├── cardset.grpc-controller.ts  # gRPC 서버 핸들러
│       │   ├── user-grpc.client.ts         # User 서비스 gRPC 클라이언트
│       │   ├── group-grpc.client.ts        # Group 서비스 gRPC 클라이언트
│       │   ├── image-grpc.client.ts        # Image 서비스 gRPC 클라이언트
│       │   └── reaction-grpc.client.ts     # Reaction 서비스 gRPC 클라이언트
│       └── persistence/
│           ├── cardset.repository.impl.ts
│           ├── card.repository.impl.ts
│           ├── cardset-manager.repository.impl.ts
│           ├── cardset-metadata.repository.impl.ts
│           ├── mapper/
│           │   ├── cardset.mapper.ts
│           │   ├── card.mapper.ts
│           │   └── cardset-manager.mapper.ts
│           └── orm/
│               ├── cardset.orm-entity.ts
│               ├── card.orm-entity.ts
│               ├── cardset-manager.orm-entity.ts
│               └── cardset-metadata.orm-entity.ts
│
├── collaboration/
│   ├── application/
│   │   └── collaboration.use-case.ts     # 협업 유즈케이스 (권한 확인, DB 로드/저장)
│   ├── domain/
│   │   ├── model/
│   │   │   └── yjs-document.ts
│   │   └── repository/
│   │       └── yjs-document.repository.ts
│   └── infrastructure/
│       ├── gateway/
│       │   └── collaboration.gateway.ts  # Socket.IO WebSocket 게이트웨이
│       ├── persistence/
│       │   ├── yjs-document.repository.impl.ts
│       │   ├── mapper/
│       │   │   └── yjs-document.mapper.ts
│       │   └── orm/
│       │       ├── yjs-document.orm-entity.ts
│       │       ├── cardset-content.orm-entity.ts   # 전체 스냅샷
│       │       └── cardset-incremental.orm-entity.ts # 증분 히스토리
│       └── redis/
│           └── yjs-document.service.ts   # Redis 기반 Yjs 문서 캐시
│
├── reaction/
│   ├── reaction.module.ts
│   └── reaction.consumer.ts              # RabbitMQ 반응 이벤트 소비
│
├── shared/
│   ├── common/
│   │   ├── api-response.ts               # 공통 응답 래퍼
│   │   ├── paged-response.ts             # 페이징 응답 래퍼
│   │   ├── business.exception.ts
│   │   ├── error-code.ts
│   │   ├── global-exception.filter.ts
│   │   └── ws-exception.filter.ts
│   ├── config/
│   │   └── auth.config.ts
│   ├── decorator/
│   │   └── ws-user.decorator.ts          # WebSocket 사용자 데코레이터
│   ├── grpc/
│   │   ├── grpc-client.module.ts
│   │   └── grpc.types.ts
│   └── types/
│       └── user-auth.type.ts
│
└── proto/
    ├── cardset.proto                     # 이 서비스가 노출하는 gRPC 계약
    ├── group.proto                       # 호출하는 Group 서비스 계약
    ├── user.proto                        # 호출하는 User 서비스 계약
    ├── image.proto                       # 호출하는 Image 서비스 계약
    └── reaction.proto                    # 호출하는 Reaction 서비스 계약
```

---

## 아키텍처 패턴

**Clean Architecture + DDD** 구조를 채택한다.

| 계층 | 디렉토리 | 역할 |
|------|----------|------|
| Domain | `domain/model`, `domain/service` | 순수 비즈니스 로직, 외부 의존 없음 |
| Application | `application/` | Use Case 오케스트레이션, 트랜잭션 경계 |
| Infrastructure | `infrastructure/` | DB, gRPC, HTTP, WebSocket 어댑터 |

---

## 데이터베이스 주요 테이블

| 테이블 | ORM Entity | 설명 |
|--------|-----------|------|
| `cardset` | `cardset.orm-entity.ts` | 카드셋 기본 정보 (name, visibility, category, hashtag, imageRefId, cardCount) |
| `card` | `card.orm-entity.ts` | 개별 카드 (content, order, cardsetId) — 현재 Yjs 방식으로 대체됨 |
| `cardset_manager` | `cardset-manager.orm-entity.ts` | 카드셋-사용자 매니저 관계 (userId, cardSetId) |
| `cardset_metadata` | `cardset-metadata.orm-entity.ts` | 카드셋 메타데이터 (cardCount 캐시 등) |
| `yjs_document` | `yjs-document.orm-entity.ts` | Yjs 문서 전체 스냅샷 (cardsetId → binary) |
| `cardset_content` | `cardset-content.orm-entity.ts` | Yjs 전체 스냅샷 저장소 |
| `cardset_incremental` | `cardset-incremental.orm-entity.ts` | Yjs 증분 업데이트 히스토리 |

---

## 실시간 협업 아키텍처

Yjs CRDT를 이용해 충돌 없는 실시간 동기화를 구현한다.

```
클라이언트 A ─┐
               ├─ Socket.IO ──► CollaborationGateway
클라이언트 B ─┘                        │
                                        ├─ Redis (Yjs 문서 캐시, 활성 클라이언트 관리)
                                        └─ MySQL (영속 저장: 스냅샷 + 증분)
```

**데이터 흐름:**
1. 클라이언트가 `join-cardset` 이벤트 전송
2. 매니저 권한 확인 (DB 조회)
3. Redis에서 Yjs 문서 로드 → 없으면 MySQL에서 복원 → 없으면 신규 생성
4. 클라이언트에 `sync` 이벤트로 전체 상태 전달
5. 편집 시 클라이언트가 `update` 이벤트로 증분 업데이트 전송
6. 서버가 병합 후 방 전체에 `sync` 브로드캐스트
7. 마지막 사용자 퇴장 후 5초 뒤 증분 히스토리를 MySQL에 flush하고 Redis 문서 삭제

---

## 외부 의존성

### gRPC 호출 (아웃바운드)

| 서비스 | 포트 | 용도 |
|--------|------|------|
| User | 9091 | 사용자 정보 조회 (매니저 프로필) |
| Group | 9094 | 그룹 멤버 확인, 그룹 정보 조회 |
| Image | 9092 | 이미지 URL 조회, 이미지 활성화 |
| Reaction | 9093 | 좋아요/북마크 수 및 여부 조회 |

### gRPC 서버 (인바운드, 포트 9095)

`CardsetService`를 노출한다:
- `IsCardSetViewable`: Reaction 서비스 등이 카드셋 접근 가능 여부 확인에 사용
- `GetCardSetsByIds`: 배치 카드셋 정보 조회

### RabbitMQ (인바운드·아웃바운드)

`reaction.consumer.ts`가 Reaction 서비스의 반응 이벤트(좋아요/북마크 수 변경)를 구독해 `cardset_metadata` 업데이트에 반영한다.

- `shared/messaging/messaging.module.ts`가 반응 이벤트와 편집 권한 이벤트의 RabbitMQ 연결·exchange 설정을 공유한다. 같은 이름의 연결을 여러 번 초기화하지 않는다.
- 매니저 교체는 삭제·추가·카드셋 갱신에 같은 EntityManager를 사용한다. 커밋 후 `editor-access.exchange` / `cardset.editor.revoked`로 `CARDSET_EDITOR_REVOKED`를 발행한다.
- `EditorAccessConsumer`는 `cardset.editor-access.queue`에서 위 이벤트와 Group의 `group.member.kicked`를 구독한다. 그룹 강퇴는 그룹 내 모든 카드셋의 매니저 행을 삭제하고 편집 세션을 철회한다. 행이 이미 없어도 소켓 퇴장을 수행하여 재전달을 처리한다.
- `CollaborationGateway`는 소켓·카드셋별 입장 상태와 편집 버퍼를 관리한다. 강퇴 시 비동기 정리에 앞서 세션을 무효화한다. Redis 인덱스의 TTL·조회 실패에 영향을 받지 않도록 현재 서버의 소켓을 직접 찾아 `kicked`를 전송한다.
- 현재 단일 Cardset 인스턴스를 전제로 한다. 여러 replica에서는 이벤트 fan-out 또는 Socket.IO adapter를 통한 전체 인스턴스 철회가 필요하다.
- DB 커밋과 MQ 발행은 별도 작업이며 outbox는 구현하지 않았다. 그 사이 장애로 인한 이벤트 유실은 보장하지 않는다. 이미 실행 중인 Redis 저장은 롤백하지 않으며, 아직 시작하지 않은 버퍼와 이후 편집을 차단한다.


---

## 환경 변수

| 변수 | 설명 |
|------|------|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS` | MySQL 연결 정보 |
| `REDIS_HOST`, `REDIS_PORT` | Redis 연결 정보 |
| `RABBITMQ_URL` | RabbitMQ 연결 URL |
| `JWT_SECRET` | JWT 서명 검증 키 |
| `GRPC_USER_URL` | User 서비스 gRPC 주소 (예: `user-service:9091`) |
| `GRPC_GROUP_URL` | Group 서비스 gRPC 주소 |
| `GRPC_IMAGE_URL` | Image 서비스 gRPC 주소 |
| `GRPC_REACTION_URL` | Reaction 서비스 gRPC 주소 |
