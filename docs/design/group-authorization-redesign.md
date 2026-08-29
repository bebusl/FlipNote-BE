# 그룹 권한 · 오너십 재설계 문서

> 상태: **제안(Draft)** · 대상 서비스: `FlipNote-Group` · 작성 기준일: 2026-07-25
> 목적: 회의에서 정한 "role별 고정 권한" 모델로 전환하고, 현재 구조의 사각지대(그룹 정보 수정 권한 부재, OWNER 양도 불가, 자발적 탈퇴 부재)를 메우기 위한 서버 변경 설계.
> ⚠️ 이 문서는 앞으로 만들 것을 정의한다. 아래 "현재 상태"는 조사 시점 코드 기준이며, 작업 전 실제 코드로 재확인할 것.

---

## 1. 현재 상태 요약 (조사 시점)

| 기능 | 엔드포인트 | 현재 인가 방식 | 문제 |
|---|---|---|---|
| 그룹 정보 수정 | `PUT /v1/groups/{groupId}` | **OWNER 전용** 하드코딩 (`ChangeGroupService`) | 대응 권한 없음. 문서엔 "OWNER or sufficient permission"이나 코드와 불일치 |
| 그룹 삭제 | `DELETE /v1/groups/{groupId}` | **OWNER 전용** (`DeleteGroupService`) | 적절. 유지 |
| 멤버 역할 변경 | `PUT /v1/groups/{groupId}/members/{memberId}` | 순수 role 계층 (`ChangeRoleService`, `isHigherThan`) | OWNER(4) 승격 불가(`4>4=false`) → **OWNER 양도 경로 없음** |
| 멤버 강퇴 | `DELETE /v1/groups/{groupId}/members/{memberId}` | MEMBER_MANAGE 또는 상위 role | — |
| OWNER 양도 | — | **없음** | 생성자가 영구 유일 OWNER. 잠수 시 그룹 좀비화 |
| 자발적 탈퇴 | — | **없음** | 강퇴(kick)만 존재. 본인이 나갈 방법 없음 |

**권한(GroupPermission) 현재값 (3개):** `MEMBER_MANAGE`, `JOIN_REQUEST_MANAGE`, `INVITE` — `GroupPermission.java`
**역할(GroupMemberRole):** `OWNER(4) > HEAD_MANAGER(3) > MANAGER(2) > MEMBER(1)` — `GroupMemberRole.java`

**핵심 리스크:** OWNER가 잠수하면 (1) 아무도 OWNER 승격 불가, (2) 아무도 그룹 삭제 불가, (3) 아무도 탈퇴 불가 → **그룹이 영구 잠김.**

---

## 2. 새 권한 모델 (role별 고정)

- **변경 전(동적):** 그룹마다 role에 권한을 부여/회수 (`POST/DELETE /v1/groups/{id}/permissions`).
- **변경 후(고정):** role마다 권한이 **코드로 고정.** 멤버 role을 바꾸면 그 role의 권한을 자동으로 획득.
- 따라서 **동적 부여/회수 API 2개(`AddPermission`/`RemovePermission`)는 폐기 또는 미사용** 처리.
- `GET /v1/groups/{groupId}/permissions` (내 role + 권한 조회)는 **유지** — 프론트가 화면 게이팅에 사용.

### 2-1. 권한 목록 (기존 3 + 신규 1)

| 권한 | 의미 | 상태 |
|---|---|---|
| `GROUP_MANAGE` | 그룹 정보(이름/설명 등) 수정 | **신규 추가** |
| `MEMBER_MANAGE` | 멤버 강퇴 | 기존 |
| `JOIN_REQUEST_MANAGE` | 가입 신청 수락/거절 | 기존 |
| `INVITE` | 초대 발송 | 기존 |

> **그룹 삭제·OWNER 양도는 권한(permission)으로 만들지 않는다.** OWNER 전용 특수 동작으로 남긴다 (파괴적·유일성 동작이라 권한으로 하위 role에 새어나가면 위험).

### 2-2. role → 권한 고정 매핑 (제안, 확정 필요)

| 권한 \ role | OWNER | HEAD_MANAGER | MANAGER | MEMBER |
|---|:---:|:---:|:---:|:---:|
| `GROUP_MANAGE` | ✓ | ✓ | ✗ | ✗ |
| `MEMBER_MANAGE` | ✓ | ✓ | ✗ | ✗ |
| `JOIN_REQUEST_MANAGE` | ✓ | ✓ | ✓ | ✗ |
| `INVITE` | ✓ | ✓ | ✓ | ✗ |
| 그룹 삭제 (특수) | ✓ | ✗ | ✗ | ✗ |
| OWNER 양도 (특수) | ✓ | ✗ | ✗ | ✗ |

> MANAGER에게 `MEMBER_MANAGE`/`GROUP_MANAGE`를 줄지는 팀 결정 사항. 위 표는 "HEAD_MANAGER는 관리 권한 대부분 보유, MANAGER는 초대·가입승인 위주"를 가정한 초안.

---

## 3. API별 게이팅 기준 (변경 후)

| 기능 | 게이팅 기준 | 실패 에러(안) |
|---|---|---|
| 그룹 정보 수정 | `GROUP_MANAGE` 권한 보유 | 403 |
| 그룹 삭제 | OWNER 여부 (특수) | 403 NOT_OWNER |
| 멤버 역할 변경 | role 계층 유지: 요청자 role > max(부여할 role, 대상 현재 role). 단 **OWNER로의 승격은 여기서 계속 차단** (양도 전용 API로만) | 403 |
| 멤버 강퇴 | `MEMBER_MANAGE` 권한 + (대상보다 상위 role 권장) | 403 |
| 초대 발송 | `INVITE` 권한 | 403 |
| 가입 신청 응답 | `JOIN_REQUEST_MANAGE` 권한 | 403 |

**프론트 게이팅:** `GET /permissions`의 `permissions` 배열로 판정 (`permissions.includes("GROUP_MANAGE")` 등).
단 **그룹 삭제 탭 / 역할 변경 탭**은 배열로 안 됨 → 삭제는 `role === "OWNER"`, 역할 변경은 내 role과 **대상 멤버 role 비교**(멤버 목록 API가 각 멤버 role을 내려줘야 함 — 확인 필요).

---

## 4. OWNER 양도 (신규)

**목적:** 잠수 대비 1차 방어 + 정상 인수인계.

- **신규 엔드포인트(안):** `PUT /v1/groups/{groupId}/owner` (또는 `POST .../transfer-ownership`)
- **요청자:** 현재 OWNER만
- **대상:** 같은 그룹의 멤버 1명
- **처리:** 대상 → OWNER 승격, 기존 OWNER → 한 단계 낮은 role(예: HEAD_MANAGER)로 강등. 그룹당 OWNER는 항상 정확히 1명 유지.
- **주의:** 역할 변경 API(`ChangeRoleService`)로는 OWNER 승격이 구조상 막혀 있으므로(`isHigherThan` `>` 비교), **별도 서비스/경로로 구현**해야 함.

> 자동 승계(OWNER N일 미접속 시 HEAD_MANAGER 자동 승계)는 "마지막 접속 시간" 추적이 필요해 현 스코프 밖. **후속 과제**로 분리.

---

## 5. 자발적 탈퇴 (신규)

**신규 엔드포인트(안):** `DELETE /v1/groups/{groupId}/members/me` (나가기)

역할에 따라 흐름이 다르다.

### 5-1. MEMBER — 즉시 탈퇴
승인 없이 바로 그룹에서 제거.

### 5-2. 책임자(HEAD_MANAGER / MANAGER) — 승인제 탈퇴
1. 본인이 **탈퇴 요청** 생성 (pending 상태)
2. **OWNER 또는 요청자보다 상위 role**이 confirm
3. confirm 시 대상 role을 **MEMBER로 강등 후 탈퇴 처리**(그룹에서 제거)
   - 구현상으로는 "승인 → 제거"로 원자적 처리 가능. 강등은 책임 해제를 명시하기 위한 개념적 단계.

> 승인자를 OWNER로만 좁히지 말 것. "요청자보다 상위 role 누구나 승인 가능"으로 해야 OWNER 잠수 시에도 하위 책임자가 나갈 여지가 생긴다.

### 5-3. OWNER — 탈퇴 불가 (양도 선행)
- OWNER는 이 흐름을 쓸 수 없다.
- 반드시 **OWNER 양도(§4)를 먼저** 수행 → MEMBER가 된 뒤 §5-1로 탈퇴.
- 이유: OWNER 없는 그룹 방지.

### 5-4. 알려진 한계
- HEAD_MANAGER의 탈퇴 요청은 상위가 OWNER뿐 → **OWNER 잠수 시 HEAD_MANAGER는 여전히 못 나감.**
- 이는 자동 승계(후속 과제) 도입 전까지 남는 엣지 케이스로 수용.

---

## 6. 서버 변경 체크리스트

- [ ] `GroupPermission` enum에 `GROUP_MANAGE` 추가
- [ ] role → 권한 고정 매핑 정의 (코드 상수/enum 매핑 또는 초기 시드). §2-2 확정 후
- [ ] `ChangeGroupService`: OWNER-only → `GROUP_MANAGE` 권한 체크로 변경
- [ ] 동적 권한 API(`AddPermissionService`/`RemovePermissionService`, `POST/DELETE /permissions`) 폐기 또는 비활성
- [ ] `DeleteGroupService`: OWNER 전용 유지 (변경 없음)
- [ ] **OWNER 양도** 엔드포인트/서비스 신규 (§4)
- [ ] **자발적 탈퇴** 엔드포인트/서비스 신규 + 책임자 탈퇴 요청/승인 흐름 (§5)
- [ ] `GET /permissions` 응답 형태 유지 확인 (`{ role, permissions[] }`)
- [ ] 멤버 목록 API가 각 멤버 role을 내려주는지 확인 (역할 변경 탭 프론트 게이팅 의존) — **미확인**
- [ ] API 문서 갱신 (`docs/api/ai`, `docs/api/human`) + 아키텍처 문서 반영

---

## 7. 열린 결정 사항

1. §2-2 role별 권한 매핑 확정 (MANAGER에게 MEMBER_MANAGE/GROUP_MANAGE 줄지)
2. OWNER 양도 시 기존 OWNER의 강등 role (HEAD_MANAGER? MEMBER?)
3. 책임자 탈퇴 승인제를 실제로 둘지 vs "즉시 탈퇴 + 알림"으로 단순화할지
4. 신규 엔드포인트 경로/네이밍 확정
5. 자동 승계(잠수 대비 B안) 후속 과제 편입 여부
