# 로컬 실행 환경 가이드

FlipNote 백엔드 7개 서비스 + 인프라(MySQL/Redis/RabbitMQ)를 로컬에서 한 번에 띄우는 방법.

> **이건 로컬 개발 전용입니다.** 실제 배포는 `FlipNote-Infra`의 ArgoCD + Helm + Vault(GitOps)로 이뤄지며 이 compose와 무관합니다.
> k8s로 배포하는 팀이 로컬은 docker-compose를 따로 두는 건 흔한 구성입니다 — 반복 속도와 리소스 때문입니다.

---

## 1. 전제 조건

| 항목 | 요구사항 |
|---|---|
| Docker Desktop | 실행 중이어야 함 |
| 메모리 할당 | **12GB 이상 권장** (Settings → Resources) |

모든 애플리케이션 서비스는 로컬 소스와 Dockerfile로 빌드하므로, Apple Silicon에서는 네이티브 아키텍처 이미지로 실행됩니다.

---

## 2. 최초 1회 셋업

```bash
cp .env.example .env
```

`.env`에 값을 채웁니다. **`.env`는 `.gitignore`에 등록돼 있으니 절대 커밋하지 마세요.**

| 변수 | 필수 | 설명 |
|---|:---:|---|
| `MYSQL_ROOT_PASSWORD` | ✓ | 아무 값이나. 컨테이너 내부에서만 씀 |
| `JWT_SECRET` | ✓ | 32자 이상 랜덤 문자열. `openssl rand -hex 32`<br>**user-service와 cardset-service가 공유합니다** |
| `RESEND_API_KEY` | ✓ | 없으면 user/group 서비스가 기동 실패. 메일 안 쓸 거면 더미 문자열이라도 넣을 것 |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | ✓ | 서비스 계정 JSON **전체를 한 줄로**. 형식이 깨지면 notification-service가 기동 실패 |
| `GOOGLE_CLIENT_ID` / `_SECRET` | | 구글 소셜 로그인 테스트할 때만 |
| `S3_*` | | 이미지 업로드 테스트할 때만 |

### `FIREBASE_SERVICE_ACCOUNT_JSON` 주의

`FirebaseConfig`의 `@PostConstruct`가 시작 시점에 이 JSON을 파싱합니다.
`{}` 같은 빈 값을 넣으면 `GoogleCredentials.fromStream()`이 예외를 던져 **notification-service가 부팅되지 않습니다.**

푸시 알림을 실제로 테스트하지 않는다면, 형식만 유효한 더미 키를 만들어 넣으면 됩니다:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out /tmp/dummy.pem
python3 -c "
import json
sa={'type':'service_account','project_id':'flipnote-local',
    'private_key_id':'dummy','private_key':open('/tmp/dummy.pem').read(),
    'client_email':'dummy@flipnote-local.iam.gserviceaccount.com',
    'client_id':'0','token_uri':'https://oauth2.googleapis.com/token'}
print(json.dumps(sa,separators=(',',':')))"
```

출력된 한 줄을 `.env`에 넣습니다. 서비스는 정상 기동하고 알림 조회 API도 동작하지만, 실제 푸시 발송은 실패합니다.

---

## 3. 실행

```bash
# 전체 기동
docker compose up -d --build

# 인프라만 먼저 (문제 격리할 때 유용)
docker compose up -d mysql redis rabbitmq

# 상태 확인 — 앱 서비스에는 healthcheck가 없으므로 Up이 곧 준비완료는 아님
docker compose ps

# 기동 완료 확인은 로그로
docker compose logs -f user-service

# 중지 (데이터 유지)
docker compose stop

# 완전 삭제 (DB 데이터까지 날아감)
docker compose down -v
```

앱 서비스 6개는 JVM이라 첫 기동에 15~25초 걸립니다.
`api-gateway`는 `depends_on`이 짧은 형태라 뒤쪽 서비스보다 먼저 뜹니다 — 초반 30초 정도 502/503은 정상입니다.

---

## 4. 포트

| 대상 | 주소 | 비고 |
|---|---|---|
| **API Gateway** | `http://localhost:8080` | **모든 클라이언트 요청은 여기로** |
| MySQL | `127.0.0.1:3307` | DBeaver 등 GUI용. 계정은 `root` / `MYSQL_ROOT_PASSWORD` |
| RabbitMQ 관리 UI | `http://localhost:15672` | `guest` / `guest` |

개별 서비스(8081~8086)는 외부에 노출하지 않습니다. Gateway를 통해서만 접근하세요.
DB는 서비스별로 분리돼 있습니다 — `flipnote_user`, `flipnote_cardset` 등 6개.

---

## 5. 인증 방식

**Bearer 헤더가 아니라 쿠키입니다.** 로그인 성공 시 `accessToken` / `refreshToken` 쿠키가 내려오고,
Gateway의 `AuthenticationFilter`가 이걸 읽어 user-service에 검증을 요청한 뒤
`X-User-Id` / `X-User-Email` / `X-User-Role` 헤더로 바꿔 내부 서비스에 전달합니다.

프론트엔드에서는 `fetch(..., { credentials: 'include' })`가 필요합니다.

> **Safari에서는 로컬 로그인이 안 됩니다.** 쿠키가 `Secure` 플래그로 하드코딩돼 있는데
> (`CookieUtil.java`), Safari는 `http://localhost`에서 Secure 쿠키를 거부합니다.
> Chrome/Firefox는 localhost 예외가 있어 정상 동작합니다. **로컬 테스트는 Chrome을 쓰세요.**

---

## 6. 이메일 인증 우회

회원가입 전에 이메일 인증이 필요한데, FlipNote는 **SMTP가 아니라 Resend HTTP API**로 메일을 보냅니다.
따라서 **MailHog 같은 SMTP 인터셉터로는 가로챌 수 없습니다.**

기본 Docker Compose 설정은 `MAIL_MODE=console`이라 인증 코드를 실제로 발송하지 않고 User 서비스 로그에 표시합니다. Docker Desktop에서 `user-service` 로그를 열거나 아래 명령으로 확인하세요:

```bash
docker compose logs -f user-service
# [DEV EMAIL] Verification code for test@flipnote.test: 123456 (valid for 10 minutes)
```

인증 코드는 Redis에도 저장됩니다. 자동화가 필요하면 헬퍼 스크립트를 쓸 수 있습니다:

```bash
./scripts/verify-email.sh test@flipnote.test
# → 인증 완료: test@flipnote.test (code=123456, 10분 내에 회원가입 요청을 보내세요)
```

인증 완료 상태는 **10분**만 유지되므로 그 안에 회원가입을 마쳐야 합니다.

전체 흐름 예시:

```bash
./scripts/verify-email.sh test@flipnote.test

curl -X POST http://localhost:8080/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@flipnote.test","password":"local1234","name":"테스트",
       "nickname":"테스트","smsAgree":false,"phone":"01012345678"}'

curl -c cookies.txt -X POST http://localhost:8080/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@flipnote.test","password":"local1234"}'

curl -b cookies.txt http://localhost:8080/v1/users/me
```

---

## 7. 전체 동작 확인 (스모크 테스트)

아래 순서대로 돌리면 서비스 간 gRPC 연동까지 한 번에 검증됩니다. 실제로 검증된 페이로드입니다.

```bash
# 로그인해서 토큰 확보
TOKEN=$(curl -s -D - -o /dev/null -X POST http://localhost:8080/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@flipnote.test","password":"local1234"}' \
  | grep -i "set-cookie: accessToken=" | sed 's/.*accessToken=\([^;]*\).*/\1/' | tr -d '\r')

# 그룹 생성 — group-service → user/image gRPC
curl -s -X POST http://localhost:8080/v1/groups -H "Cookie: accessToken=$TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"테스트그룹","category":"IT","description":"설명","joinPolicy":"OPEN","visibility":"PUBLIC","maxMember":10}'

# 카드셋 생성 — cardset-service → user/group gRPC
curl -s -X POST http://localhost:8080/v1/card-sets -H "Cookie: accessToken=$TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"테스트 카드셋","groupId":1,"visibility":"PUBLIC","category":"언어"}'

# 카드셋 목록 — 응답의 likeCount/bookmarked는 reaction gRPC, managers는 user gRPC 경유
curl -s "http://localhost:8080/v1/card-sets?page=1&size=10" -H "Cookie: accessToken=$TOKEN"
```

> **주의:** `zsh`에서 `GID`는 예약 변수입니다. 스크립트에서 그룹 ID를 담을 때 `GID` 대신 다른 이름을 쓰세요.

### 필드명 주의

- 그룹 생성은 `category` / `joinPolicy` / `visibility` / `maxMember`가 **전부 필수**입니다. 누락 시 500이 납니다.
- 카드셋 생성의 제목 필드는 `title`이 아니라 **`name`** 입니다. 잘못 보내면 DB 레벨에서 500이 납니다.

---

## 8. 트러블슈팅

### 변경한 소스가 컨테이너에 반영되지 않음

Compose의 모든 애플리케이션 서비스는 GHCR 이미지를 pull하지 않고 현재 작업 트리에서 빌드합니다. 소스를 수정한 뒤에는 다음 명령으로 재빌드·재생성하세요.

```bash
docker compose up -d --build
```

`pull_policy: build`가 설정되어 있어 GHCR의 같은 이름·태그 이미지를 사용하지 않습니다. Docker 빌드 캐시는 유지되며, 변경된 소스가 있는 빌드 단계만 다시 실행됩니다.

### notification-service가 계속 재시작함

`FIREBASE_SERVICE_ACCOUNT_JSON` 값이 비었거나 형식이 깨진 경우입니다. 2번 항목 참고.

### WebSocket 실시간 협업이 안 됨

cardset-service는 Gateway를 거치지 않고 **자체적으로 JWT를 검증**합니다
(`auth.service.ts`의 `jwt.verify`). `.env`의 `JWT_SECRET`이 user-service와 **동일한 값**이어야 합니다.

### MySQL 데이터를 초기화하고 싶을 때

```bash
docker compose down -v && docker compose up -d --build
```

`docker/mysql/init.sql`이 서비스별 DB 6개를 다시 만들고, 각 서비스가 테이블을 자동 생성합니다
(JPA `ddl-auto: update` / TypeORM `synchronize: true`).

---

## 8. 참고

- 서비스별 아키텍처: `docs/architecture/`
- API 스펙: `docs/api/human/` (한국어), `docs/api/ai/` (LLM용)
- 배포(k8s) 구성: `FlipNote-Infra/`
