# 개인 레포 이관 및 배포 가이드

팀 프로젝트(`FlipNoteTeam` org)로 개발된 FlipNote 백엔드를 **개인 소유 레포로 옮기고 새로 배포**하기 위한 실행 문서.

> 이 문서의 모든 파일 경로와 라인 번호는 2026-08-27 기준 실제 코드에서 확인한 값이다.

---

## 이 문서의 전제

1. **배포 방식**: VM 1대 + k3s, 데이터스토어(MySQL/Redis/RabbitMQ)도 같은 클러스터 안에 Helm으로 설치.
   관리형(RDS/ElastiCache/CloudAMQP)을 쓰면 Phase 4만 달라지고 나머지는 동일하다.
2. **레포 소유자**: 모노레포와 Infra 레포 모두 **본인 GitHub 계정** 밑에 생성.
   org 밑에 만들면 GitHub Actions의 `${{ github.actor }}`와 ghcr 네임스페이스가 어긋나 첫 빌드가 실패한다.
3. **코드 자체는 그대로 올려도 안전**하다. 8개 레포 전체 히스토리(773 커밋)를 스캔했고 실제 크레덴셜은 없다.
   유일한 하드코딩 시크릿은 `FlipNote-User/src/test/resources/application.yml:29`와
   `FlipNote-Gateway/src/test/resources/application.yml:2`의 JWT 테스트 픽스처인데, 운영 값이 아니므로 무해하다.

---

## Phase 0 — 먼저 결정해야 하는 2가지 (블로커)

이 두 개가 정해지기 전에는 Phase 2 이후를 진행할 수 없다. 나머지 거의 모든 설정이 여기서 파생된다.

### ① 도메인

```
결정: ________________________  (예: api.mydomain.dev)
```

이게 정해져야 아래가 전부 확정된다.

- Ingress host (`api-gateway/values.yaml:20`)
- **Google OAuth redirect URI** — GCP 콘솔에 등록하는 값. HTTPS 필수
- Resend 발신 도메인 인증
- TLS 인증서 발급 대상

도메인 없이 IP만으로 가는 것도 기술적으로는 가능하지만, **Google OAuth가 HTTPS redirect URI를 요구**하므로 소셜 로그인을 쓸 거면 도메인이 사실상 필수다.

### ② ghcr 네임스페이스

```
결정: ghcr.io/bebusl  (public packages)
```

현재 이미지는 전부 `ghcr.io/dungbik/*` — 팀원 개인 계정 네임스페이스라 본인 PAT으로는 push 권한이 없다.

---

## Phase 1 — 레포 이관

### 1-1. 목표 구조

| 레포 | 내용 | 커밋 주체 |
|---|---|---|
| `FlipNote-BE` (모노레포) | 7개 서비스 소스 + `docs/` + `docker-compose.yml` | 사람 |
| `FlipNote-Infra` | `argocd/`, `charts/` | 사람 + argocd-image-updater(봇) |

**Infra는 반드시 분리 유지.** `argocd/apps/*.yaml:11`의 `write-back-method: git` 때문에 image-updater가 Infra 레포에 커밋을 밀어넣는다. 코드와 같은 레포에 두면:

```
코드 push → CI 빌드 → 새 digest → image-updater가 values.yaml 커밋
  → 그게 main push → CD 재실행 → 무한 루프
```

### 1-2. 모노레포 합치기

히스토리를 보존하려면 서비스마다:

```bash
git remote add user-origin https://github.com/FlipNoteTeam/FlipNote-User.git
git subtree add --prefix=FlipNote-User user-origin main
```

버려도 되면 파일 복사 후 초기 커밋. 사이드 프로젝트면 subtree 추천 — proto 변경 같은 걸 한 PR에 묶을 수 있는 게 모노레포의 실익인데, 히스토리까지 살리면 그 이득이 커진다.

### 1-3. CI/CD 워크플로 수정

기존 서비스별 `.github/workflows/cd.yml`은 각 서비스 레포를 전제로 작성돼 있었다. 모노레포에서는 루트
`.github/workflows/cd-*.yml`을 사용해야 하므로 이 작업본에 서비스별 경로 필터가 있는 워크플로를 추가했다.
대표적인 기존 형태는 다음과 같다:

```yaml
on:
  push:
    branches: [main]     # ← 기존에는 paths 필터 없음
...
      with:
      context: .        # ← 기존에는 레포 루트
```

모노레포에서 이대로 두면 **README 한 줄 고쳐도 7개 이미지가 전부 재빌드**되고, `update-strategy: digest` 때문에 image-updater가 7개 values.yaml을 전부 bump해서 플랫폼 전체가 재배포된다.

**서비스마다 이렇게 바꾼다** (파일명도 `cd-user.yml` 식으로 리네임 — 한 디렉토리에 모이면서 충돌):

```yaml
name: CD user-service

on:
  push:
    branches: [main]
    paths:
      - 'FlipNote-User/**'
      - '.github/workflows/cd-user.yml'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GHCR_PAT }}
      - id: meta
        uses: docker/metadata-action@v5
        with:
          images: ghcr.io/bebusl/flipnote-user
      - uses: docker/build-push-action@v6
        with:
          context: ./FlipNote-User
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
```

**Dockerfile은 안 고쳐도 된다.** 7개 전부 자기 디렉토리 안만 `COPY` 하고 있어서(`gradlew`, `gradle/`, `build.gradle*`, `src`, `package*.json`) 빌드 컨텍스트만 바꾸면 그대로 동작한다.

`GHCR_PAT`에는 `bebusl` 패키지의 `packages:write` 권한이 필요하다. 코드 저장소와 GHCR
네임스페이스의 소유자가 다르면 `GITHUB_TOKEN`으로는 push할 수 없다. public 패키지로 운영하므로
k8s image pull secret은 필요 없다.

---

## Phase 2 — 하드코딩 치환

### 2-1. `repoURL` — 10곳

전부 `https://github.com/FlipNoteTeam/FlipNote-Infra.git` → 본인 Infra 레포 URL.

| 파일 | 라인 |
|---|---|
| `argocd/root-apps.yaml` | 11 |
| `argocd/root-infra.yaml` | 11 |
| `argocd/infra/vault-config.yaml` | 9 |
| `argocd/apps/user-service.yaml` | 15 |
| `argocd/apps/api-gateway.yaml` | 15 |
| `argocd/apps/group-service.yaml` | 15 |
| `argocd/apps/image-service.yaml` | 15 |
| `argocd/apps/cardset-service.yaml` | 15 |
| `argocd/apps/reaction-service.yaml` | 15 |
| `argocd/apps/notification-service.yaml` | 15 |

> ⚠️ **`argocd/infra/external-secrets.yaml:9`의 `repoURL: https://charts.external-secrets.io`는 건드리지 말 것.**
> 이건 외부 Helm 차트 저장소다. 일괄 `sed`로 밀면 여기까지 바뀐다.

### 2-2. 이미지 레지스트리 — 14곳

`values.yaml`의 `repository:`(라인 4)와 `argocd/apps/*.yaml`의 `image-list` 애노테이션(라인 7), **두 곳을 항상 짝으로** 바꿔야 한다. 하나만 바꾸면 image-updater가 엉뚱한 이미지를 감시한다.

| 서비스 | 현재 값 |
|---|---|
| user-service | `ghcr.io/dungbik/flipnote-user` |
| api-gateway | `ghcr.io/dungbik/flipnote-api-gateway` |
| group-service | `ghcr.io/dungbik/flipnote-group` |
| cardset-service | `ghcr.io/dungbik/flipnote-cardset` |
| reaction-service | `ghcr.io/dungbik/flipnote-reaction` |
| notification-service | `ghcr.io/dungbik/flipnote-notification` |
| image-service | `ghcr.io/<내계정>/flipnote-image` |

> 이 작업본의 루트 `cd-image.yml`은 다른 서비스와 동일하게 GHCR로 통일했다. Infra 레포의 `values.yaml:4`,
> `image-list`도 같은 개인 계정 네임스페이스로 맞춰야 한다.

### 2-3. 도메인 — 8곳 (2종류로 나뉨)

**설정 파일 (값만 바꾸면 됨)**

| 파일 | 라인 | 현재 |
|---|---|---|
| `FlipNote-Infra/charts/apps/api-gateway/values.yaml` | 20 | `host: api3.flipnote.site` |
| `FlipNote-Infra/charts/infra/vault-config/values.yaml` | 2 | `https://vault.flipnote.site:8200` (Vault 안 쓰면 무시) |
| `FlipNote-Group/src/main/resources/application.yml` | 67 | `${RESEND_FROM_EMAIL:...}` — 환경변수로 덮기 가능 |
| `FlipNote-Group/src/main/resources/application.yml` | 69 | `${APP_CLIENT_URL:https://flipnote.site}` — 환경변수로 덮기 가능 |
| `FlipNote-User/src/main/resources/application.yml` | 79 | `${OAUTH_BASE_URL:https://api3.flipnote.site}` — 환경변수로 덮기 가능 |

**소스 코드 수정 필요 (환경변수가 없음)** → Phase 3에서 처리

| 파일 | 라인 |
|---|---|
| `FlipNote-User/src/main/java/.../config/SwaggerConfig.java` | 24 |
| `FlipNote-Group/src/main/java/.../config/SwaggerConfig.java` | 24 |
| `FlipNote-User/src/main/resources/application.yml` | 68 |

---

## Phase 3 — 코드 수정 4건

### 3-1. ⚠️ Resend 발신 주소 (User) — 안 고치면 이메일 인증이 조용히 실패

```yaml
# FlipNote-User/src/main/resources/application.yml:68
from-email: ${RESEND_FROM_EMAIL:FlipNote <no-reply@localhost>}
```

Resend는 **인증되지 않은 도메인에서의 발송을 거부**한다. 이 상태로 배포하면 회원가입은 200으로 성공하는데 인증 메일이 영원히 안 온다 — 원인 찾기 어려운 종류의 실패다.

```yaml
from-email: ${RESEND_FROM_EMAIL:FlipNote <no-reply@localhost>}
```

Group 서비스도 같은 방식으로 환경변수화되어 있으니 `RESEND_FROM_EMAIL` 값만 주면 된다.

### 3-2. Swagger 서버 URL (User, Group)

```java
// SwaggerConfig.java:24 — 양쪽 서비스 동일
new Server().url("https://api3.flipnote.site").description("Production"),
```

이 레포의 다른 외부화 값들과 동일한 방식(`${OAUTH_BASE_URL:...}`, `${DDL_AUTO:validate}`)으로 맞춘다:

```java
@Value("${swagger.server-url:http://localhost:8080}")
private String swaggerServerUrl;
...
new Server().url(swaggerServerUrl).description("Configured"),
```

```yaml
# application.yml
swagger:
  server-url: ${SWAGGER_SERVER_URL:http://localhost:8080}
```

기능에는 영향 없고 Swagger UI의 "Try it out"만 깨진다. 우선순위 낮음.

### 3-3. ⚠️ Cardset의 `synchronize` 하드코딩

```ts
// FlipNote-Cardset/src/app.module.ts:34
synchronize: process.env.DB_SYNCHRONIZE === 'true',
```

`docker-compose.yml`은 `DB_SYNCHRONIZE`를 전달하며, 이제 해당 값을 실제로 읽는다. 운영에서는 `false`를 사용한다.

TypeORM의 `synchronize`는 JPA의 `ddl-auto: update`와 달리 **컬럼/테이블 삭제까지 실행**한다. 엔티티 리팩터링 한 번에 카드 데이터가 날아갈 수 있다.

```ts
synchronize: process.env.DB_SYNCHRONIZE === 'true',
```

### 3-4. Helm 차트 — Vault 없이 시크릿 주입

현재 구조:

```yaml
# charts/apps/*/templates/deployment.yaml:48-52
{{- if .Values.externalSecret.enabled }}     # ← envFrom이 여기 묶여 있음
envFrom:
  - secretRef:
      name: {{ include "user-service.fullname" . }}-secret
{{- end }}
```

그리고 **`templates/secret.yaml` 템플릿이 존재하지 않는다.** values.yaml의 `secret: {enabled: false, data: {}}` 블록은 아무도 읽지 않는 죽은 설정이다.

따라서 `externalSecret.enabled: false`로만 바꾸면 → `envFrom`이 통째로 사라짐 → **파드에 환경변수가 하나도 안 들어가서 기동 실패.**

**7개 차트 전부 이렇게 수정한다** (아래는 그대로 복붙 가능 — 차트별로 고칠 부분이 없다):

```yaml
{{- if or .Values.externalSecret.enabled .Values.existingSecret }}
envFrom:
  - secretRef:
      name: {{ .Values.existingSecret | default (printf "%s-secret" .Release.Name) }}
{{- end }}
```

> 원본의 `include "user-service.fullname" .`을 그대로 복사하면 나머지 6개 차트에서 템플릿 렌더링이 실패한다.
> 차트마다 `group-service.fullname`, `api-gateway.fullname` 처럼 이름이 다르기 때문이다.
> `.Release.Name`은 7개 차트에서 동일하고, Phase 6-1의 "Service 이름 = 릴리스 이름 = ArgoCD App 이름" 규칙과도 일치한다.

values.yaml:

```yaml
existingSecret: "user-service-secret"
externalSecret:
  enabled: false
```

시크릿은 `kubectl`로 직접 만든다(Phase 5). **git에 평문이 들어가지 않는다** — Vault를 안 쓰면서도 시크릿을 커밋하지 않는 방법이다.

---

## Phase 4 — 클러스터 및 데이터스토어

> **주의: MySQL / Redis / RabbitMQ 차트는 Infra 레포에 존재하지 않는다.**
> `charts/infra/`에는 `vault-config` 하나뿐이고, 219커밋 전체 히스토리를 뒤져도 데이터스토어 매니페스트가 없다.
> 팀은 관리형 서비스나 수동 설치로 운영했고 그 설정이 코드로 남아있지 않다. **이 부분은 새로 만들어야 한다.**

### 4-1. 클러스터 기본

```bash
curl -sfL https://get.k3s.io | sh -
kubectl create namespace flipnote

# ArgoCD
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# ingress-nginx (차트가 className: nginx 를 기대함)
helm install ingress-nginx ingress-nginx/ingress-nginx -n ingress-nginx --create-namespace

# cert-manager (TLS. Google OAuth가 HTTPS를 요구하므로 필수)
helm install cert-manager jetstack/cert-manager -n cert-manager --create-namespace \
  --set installCRDs=true
```

### 4-2. ⚠️ TLS 설정 추가

현재 `charts/apps/api-gateway/values.yaml:24`가 `tls: []` — **인그레스가 평문 HTTP로만 서비스한다.**
Google OAuth redirect URI는 HTTPS를 요구하므로 이건 선택이 아니라 필수 경로다.

```yaml
ingress:
  enabled: true
  className: nginx
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
  hosts:
    - host: <내도메인>
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: flipnote-tls
      hosts:
        - <내도메인>
```

### 4-3. 데이터스토어 3종

```bash
helm install mysql bitnami/mysql -n flipnote -f mysql-values.yaml
helm install redis bitnami/redis -n flipnote
helm install rabbitmq bitnami/rabbitmq -n flipnote
```

`mysql-values.yaml` — DB 6개를 초기 생성한다. 아래 내용은 `docker/mysql/init.sql`과 동일하다:

```yaml
auth:
  rootPassword: "<생성>"
initdbScripts:
  init.sql: |
    CREATE DATABASE IF NOT EXISTS flipnote_user         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    CREATE DATABASE IF NOT EXISTS flipnote_image        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    CREATE DATABASE IF NOT EXISTS flipnote_reaction     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    CREATE DATABASE IF NOT EXISTS flipnote_group        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    CREATE DATABASE IF NOT EXISTS flipnote_cardset      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    CREATE DATABASE IF NOT EXISTS flipnote_notification CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**테이블 DDL은 작성할 필요 없다.** 마이그레이션 도구(Flyway/Liquibase)가 없고, 엔티티에서 ORM이 자동 생성한다(Phase 6).

---

## Phase 5 — 외부 계정 및 시크릿

### 5-1. 본인 명의로 새로 발급해야 하는 것

YAML 수정보다 이쪽이 실제 일정을 좌우한다. Phase 0의 도메인이 확정돼야 진행 가능한 항목이 있다.

| 항목 | 작업 | 도메인 의존 |
|---|---|---|
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | GCP 콘솔 → OAuth 클라이언트 생성, redirect URI 등록 | ✅ |
| `RESEND_API_KEY` | Resend 가입 + **발신 도메인 DNS 인증** | ✅ |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Firebase 프로젝트 생성 → 서비스 계정 키(JSON) | — |
| `S3_ACCESS_KEY` / `S3_SECRET_KEY` / `S3_BUCKET_NAME` / `S3_BUCKET_REGION` | S3 버킷 + IAM 사용자 생성 | — |
| `JWT_SECRET` | `openssl rand -hex 64` | — |
| DB / Redis / RabbitMQ 비밀번호 | 직접 생성 | — |

### 5-2. 시크릿 생성

서비스별로 `.env` 파일을 만든 뒤(**git에 커밋 금지**):

```bash
kubectl create secret generic user-service-secret -n flipnote --from-env-file=./secrets/user.env
kubectl create secret generic image-service-secret -n flipnote --from-env-file=./secrets/image.env
kubectl create secret generic group-service-secret -n flipnote --from-env-file=./secrets/group.env
kubectl create secret generic cardset-service-secret -n flipnote --from-env-file=./secrets/cardset.env
kubectl create secret generic reaction-service-secret -n flipnote --from-env-file=./secrets/reaction.env
kubectl create secret generic notification-service-secret -n flipnote --from-env-file=./secrets/notification.env
kubectl create secret generic api-gateway-secret -n flipnote --from-env-file=./secrets/gateway.env
```

각 서비스가 요구하는 키 전체 목록은 **부록 A** 참조.

---

## Phase 6 — 첫 배포

### 6-1. ⚠️ ArgoCD Application 이름을 바꾸지 말 것

Gateway는 서비스 DNS 이름으로 직접 라우팅한다:

```yaml
# FlipNote-Gateway/src/main/resources/application.yml
uri: http://user-service:8081       # :12, :24, :40, :55
uri: http://image-service:8082      # :79, :84
uri: http://reaction-service:8083   # :66
uri: http://group-service:8084      # :98, :110, :121
uri: http://cardset-service:8085    # :91, :126, :137, :142
uri: ws://cardset-service:8085      # :155  (WebSocket)
uri: http://notification-service:8086  # :162, :172
```

그리고 7개 차트가 전부 `fullnameOverride: ""`다. 즉 **k8s Service 이름 = Helm 릴리스 이름 = ArgoCD Application 이름**.

`user-service`를 `flipnote-user`로 개명하면 Service 이름이 바뀌고, Gateway가 **원인 불명의 503**을 뱉는다. 애플리케이션 이름은 현재대로 유지한다.

gRPC 주소도 같은 규칙이다:

```
GRPC_USER_URL=user-service:9091
GRPC_IMAGE_URL=image-service:9092
GRPC_REACTION_URL=reaction-service:9093
GROUP_GRPC_URL=group-service:9094
```

### 6-2. 첫 부팅용 DDL 플래그

Reaction과 Notification은 기본값이 `validate`라 **테이블이 없으면 기동 실패**한다.

| 서비스 | 환경변수 | 첫 배포 값 | 이후 |
|---|---|---|---|
| Reaction | `DDL_AUTO` | `update` | `validate` |
| Notification | `JPA_DDL_AUTO` | `update` | `validate` |
| Cardset | `DB_SYNCHRONIZE` | `true` | `false` |
| User | `DDL_AUTO` | (기본값 `update`) | `validate` 권장 |
| Group / Image | — | `application.yml`에 `update` 하드코딩 | — |

> Cardset은 Phase 3-3의 코드 수정을 먼저 해야 `DB_SYNCHRONIZE`가 실제로 동작한다.

테이블 생성 확인 후 `validate` / `false`로 되돌리고 재배포.

**시드 데이터는 필요 없다.** Group의 `group_roles` 테이블은 `group_id` 컬럼을 가진 그룹별 데이터이고, 그룹 생성 시 앱이 기본 역할을 만든다(`GroupRoleRepositoryAdapter.java:35-45`). 빈 DB로 시작해도 정상 동작한다.

### 6-3. ArgoCD 부트스트랩

```bash
# image-updater가 Infra 레포에 write-back 하려면 쓰기 크레덴셜 등록 필요
argocd repo add https://github.com/<내계정>/FlipNote-Infra.git \
  --username <계정> --password <PAT>

# Vault를 사용할 때만 아래를 적용한다.
# kubectl apply -f argocd/root-infra.yaml
kubectl apply -f argocd/root-apps.yaml
```

> **Self-hosted without Vault:** 이 작업본은 Kubernetes Secret을 직접 주입하는 경로를 기본값으로 사용하므로
> `root-infra.yaml`은 적용하지 않아도 됩니다. `root-infra.yaml`은 External Secrets + Vault를 사용할 때만
> 적용하세요. 애플리케이션은 각 차트의 `existingSecret`을 통해 `*-service-secret`을 읽습니다.

> 크레덴셜을 등록하지 않으면 image-updater가 **에러 없이 조용히 실패**한다. 배포는 되는데 이미지가 영원히 갱신되지 않는 상태가 된다.

---

## Phase 7 — 배포 후 확인

```bash
kubectl get pods -n flipnote            # 7개 서비스 Running
kubectl logs -n flipnote deploy/user-service | grep -i "hibernate\|table"
curl https://<내도메인>/v1/auth/login    # Gateway 라우팅
```

### 확인이 필요한 미검증 항목

**RabbitMQ 토폴로지** — `CLAUDE.md` 기준으로 Group → Notification(초대/가입 요청), Reaction → Cardset(반응 이벤트) 이벤트 흐름이 존재하는데, Infra 레포에 RabbitMQ definitions 파일이 없다. 앱이 부팅 시 exchange/queue를 직접 선언하는지, 사전 생성을 기대하는지 확인되지 않았다.

→ **Group과 Reaction 첫 부팅 로그에서 connection/channel 에러를 확인할 것.** 사전 생성이 필요하면 RabbitMQ management UI 또는 definitions.json으로 만들어야 한다.

**api-gateway의 `JWT_SECRET`** — `charts/apps/api-gateway/values.yaml:50`의 externalSecret 목록에 `DB_PASSWORD` 하나뿐이다. Gateway는 JWT 검증 필터를 수행하므로 `JWT_SECRET`이 필요할 텐데 목록에 없다. **팀 개발자에게 확인할 가치가 있는 유일한 항목.** 확인이 안 되면 다른 서비스와 동일한 `JWT_SECRET`을 gateway 시크릿에도 넣고 인증 동작을 테스트한다.

---

## 부록 A — 서비스별 시크릿 키 목록

`charts/apps/*/values.yaml`의 `externalSecret.data`에서 추출. `kubectl create secret --from-env-file`에 넣을 `.env` 작성용 워크시트.

> ⚠️ **서비스마다 DB 환경변수 이름이 다르다. 오타가 아니라 실제 값이다.**
> user/reaction/notification은 `DB_URL`·`DB_USERNAME`·`DB_PASSWORD`,
> image는 `DB_URL`·`SPRING_DATASOURCE_USERNAME`·`SPRING_DATASOURCE_PASSWORD`,
> group은 `SPRING_DATASOURCE_URL`·`SPRING_DATASOURCE_USERNAME`·`SPRING_DATASOURCE_PASSWORD`.
> 통일하려고 이름을 바꾸면 Spring 바인딩이 깨진다. 각 서비스의 `application.yml`을 같이 고칠 게 아니면 아래 그대로 쓸 것.

### user-service
```
DB_URL  DB_USERNAME  DB_PASSWORD
REDIS_HOST  REDIS_PORT  REDIS_PASSWORD
GOOGLE_CLIENT_ID  GOOGLE_CLIENT_SECRET
APP_CLIENT_URL  JWT_SECRET  APP_RESEND_API_KEY
DDL_AUTO            # 첫 배포 update, 이후 validate
OAUTH_BASE_URL      # Phase 2-3
RESEND_FROM_EMAIL   # Phase 3-1 코드 수정 후
```

### image-service
```
DB_URL  SPRING_DATASOURCE_USERNAME  SPRING_DATASOURCE_PASSWORD
S3_ACCESS_KEY  S3_SECRET_KEY  S3_BUCKET_NAME  S3_BUCKET_REGION
SPRING_DATA_REDIS_HOST  SPRING_DATA_REDIS_PORT  SPRING_DATA_REDIS_PASSWORD
```

### group-service
```
SPRING_DATASOURCE_URL  SPRING_DATASOURCE_USERNAME  SPRING_DATASOURCE_PASSWORD
GRPC_IMAGE_URL  GRPC_USER_URL
RESEND_API_KEY  RESEND_FROM_EMAIL  APP_CLIENT_URL
RABBITMQ_HOST  RABBITMQ_PORT  RABBITMQ_USERNAME  RABBITMQ_PASSWORD
```

### cardset-service
```
DB_HOST  DB_PORT  DB_DATABASE  DB_USERNAME  DB_PASSWORD  DB_SYNCHRONIZE
REDIS_HOST  REDIS_PORT  REDIS_PASSWORD
RABBITMQ_URL
USER_GRPC_URL  GROUP_GRPC_URL  IMAGE_GRPC_URL  GRPC_REACTION_URL
GRPC_PORT  PORT  NODE_ENV
DEFAULT_CARDSET_IMAGE_URL
SKIP_WS_AUTH  SKIP_USER_GRPC  SKIP_REACTION_GRPC    # 운영에서는 전부 false
```

### reaction-service
```
DB_URL  DB_USERNAME  DB_PASSWORD
RABBITMQ_HOST  RABBITMQ_PORT  RABBITMQ_USERNAME  RABBITMQ_PASSWORD
DDL_AUTO            # 첫 배포 update, 이후 validate
```

### notification-service
```
DB_URL  DB_USERNAME  DB_PASSWORD
RABBITMQ_HOST  RABBITMQ_PORT  RABBITMQ_USERNAME  RABBITMQ_PASSWORD
FIREBASE_SERVICE_ACCOUNT_JSON
SPRINGDOC_SERVER_URL
JPA_DDL_AUTO        # 첫 배포 update, 이후 validate
```

### api-gateway
```
DB_PASSWORD
JWT_SECRET          # ⚠️ 원본 목록에 없음 — Phase 7 확인 항목
```

---

## 부록 B — 작업 순서 요약

| # | 작업 | 선행 조건 |
|---|---|---|
| 0 | 도메인 · ghcr 네임스페이스 결정 | — |
| 1 | 모노레포 생성 + CD 워크플로 7개 수정 | 0-② |
| 2 | Infra 레포 치환 (repoURL 10 / 이미지 14 / 도메인 8) | 0 |
| 3 | 코드 수정 4건 (Resend, Swagger, synchronize, Helm 차트) | — |
| 4 | k3s + ArgoCD + ingress + cert-manager + 데이터스토어 3종 | 0-① |
| 5 | 외부 계정 발급 + kubectl 시크릿 생성 | 0-①, 4 |
| 6 | 첫 배포 (DDL 플래그 ON → 확인 → OFF) | 1~5 |
| 7 | 검증 + RabbitMQ 토폴로지 / gateway JWT 확인 | 6 |

3번은 0번과 무관하므로 도메인 결정 전에 미리 해둘 수 있다.
