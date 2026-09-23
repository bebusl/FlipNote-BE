# Notion ChangeLog 게시 기록

대상: [FLIPNOTE-BE ChangeLog](https://app.notion.com/p/3ce69255c8398024a138f14c9dadc7be)
상태: 2026-09-23 사용자 승인 후 게시 완료. 자동 승인 검토의 최초 거부 이후 사용자가 아래 원고 게시를 명시적으로 승인했고, Notion 갱신 완료와 기존 내용 보존을 확인했다.

---

# 2026-09-23 — 편집 권한 철회 검토·수정 및 stash 커밋

별도 브랜치 `feat/editor-access-revocation`에 로컬 커밋을 완료했다. 기존 스냅샷 실습 작업과 원본 stash는 보존했으며 푸시·배포는 수행하지 않았다.

## 수정

- 강퇴 시 소켓·카드셋별 편집 세션을 무효화하여 진행 중 입장과 대기 중인 편집을 취소한다.
- 그룹 강퇴 메시지 재전달 시 매니저 행이 이미 없어도 소켓 퇴장을 재시도한다.
- Redis 인덱스가 없어도 현재 서버에 연결된 대상 소켓을 모두 찾아 철회한다.
- 매니저 교체 작업의 트랜잭션을 통일하고 RabbitMQ 연결 설정을 공유한다. 잘못된 메시지의 ID가 전체 조회·삭제 조건으로 사용되지 않도록 검증한다.
- Group은 대상 그룹·userId를 확인하고 커밋 이후에만 강퇴 이벤트를 발행한다.
- Gateway는 기존 polling 장애 복구 변경을 유지한다. polling과 WebSocket upgrade 경로를 테스트했다.
- 한국어·영문 API 및 아키텍처 문서에 동작과 프론트엔드의 `kicked` 처리 방법을 기록했다.

## 검증

- Cardset 24개, Group 6개, Gateway 3개 테스트 통과.
- Cardset 빌드·타입 검사·변경 파일 ESLint 통과.
- Group은 로컬 JDK 21에 Java 17 `--release`를 적용해 검증했으며 저장소의 Java 17 toolchain 설정은 유지한다.
- 실제 브라우저와 전체 서비스를 연결한 편집 E2E 테스트는 수행하지 않았다.

## 구현 커밋

- `a90843f` — `feat(group): publish member kick events after commit`
- `4fc36cd` — `feat(cardset): revoke active editor sessions safely`
- `bb48c9a` — `fix(gateway): preserve Socket.IO polling before websocket upgrade`

## 적용 범위

단일 Cardset 인스턴스 기준이다. 다중 인스턴스의 전체 소켓 철회, DB 커밋과 MQ 발행 사이 이벤트 유실을 복구하는 outbox는 후속 과제다. 이미 시작된 Redis 저장은 롤백하지 않으며, 아직 실행하지 않은 버퍼와 이후 편집을 차단한다.
