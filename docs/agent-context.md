# Agent 작업 컨텍스트

이 문서는 FlipNote-BE를 작업하는 에이전트가 다음 세션에서도 바로 이어갈 수 있도록,
서비스 구조와 중복되지 않는 로컬·배포·자동화·진행 이슈만 기록한다.
구조 및 API의 소스 오브 트루스는 `AGENTS.md`와 `docs/architecture/`, `docs/api/`다.

## 로컬 실행

- 루트 `docker-compose.yml`은 **로컬 개발 전용**이다. MySQL, Redis, RabbitMQ와 애플리케이션 서비스 7개를 실행한다.
- 모든 애플리케이션 서비스는 GHCR 이미지를 pull하지 않고 현재 작업 트리의 Dockerfile에서 빌드한다.
- 소스 변경 후 실행 명령: `docker compose up -d --build`
- `.env.example`을 `.env`로 복사해 사용한다. `.env`는 Git에 올리지 않는다.
- `JWT_SECRET`은 user-service와 cardset-service가 반드시 같은 값을 사용해야 한다.
- 로컬 서비스 구성과 환경 변수의 상세 설명은 `docs/local-setup.md`를 따른다.

## 운영 배포

- 운영은 Docker Compose를 사용하지 않는다. 흐름은 `main` push → GitHub Actions → GHCR → ArgoCD Image Updater → Helm → Kubernetes 롤링 업데이트다.
- 실제 GitHub Actions 실행 대상은 루트 `.github/workflows/`뿐이다. `FlipNote-*/.github/workflows/`의 이전 중첩 워크플로는 제거됐다.
- CD 워크플로: `.github/workflows/cd-*.yml`
  - 서비스 디렉터리별 `paths` 필터를 사용한다.
  - `GITHUB_TOKEN`과 `permissions: packages: write`로 `ghcr.io/bebusl/flipnote-*`에 push한다.
  - 별도의 `ORG_PAT`를 만들거나 등록할 필요가 없다.
- GitOps 설정:
  - Helm Chart: `FlipNote-Infra/charts/apps/{service}/`
  - ArgoCD Application 및 Image Updater: `FlipNote-Infra/argocd/apps/`
  - 두 설정은 이미 `ghcr.io/bebusl/flipnote-*`를 참조한다. 일반 서비스 코드 변경에 맞춰 수정하지 않는다.
- GHCR 패키지를 private으로 운영할 경우에만 Kubernetes와 ArgoCD Image Updater에 읽기 권한(image pull secret)이 필요하다.

## CI

- 루트 CI 워크플로는 다음 다섯 서비스에 있다.
  - `ci-user.yml`, `ci-reaction.yml`, `ci-notification.yml`, `ci-gateway.yml`: Gradle 빌드와 테스트
  - `ci-cardset.yml`: MySQL·Redis 서비스 컨테이너, Node 빌드·lint·단위/E2E/커버리지 테스트
- Group과 Image에는 현재 CI가 없다. 테스트 범위가 정해질 때 루트 `.github/workflows/`에 추가한다.
- 중첩 워크플로 파일을 다시 만들지 않는다. GitHub Actions가 모노레포 하위 경로를 워크플로 디렉터리로 인식하지 않기 때문이다.

## ChangeLog

- 환경·배포·CI/CD·운영 동작에 영향을 주는 변경은 작업 완료 시 아래 Notion ChangeLog에 **한국어**로 기록한다.
- ChangeLog: <https://app.notion.com/p/FLIPNOTE-BE-ChangeLog-3ce69255c8398024a138f14c9dadc7be?source=copy_link>
- Helm과 ArgoCD 설명 하위 문서: <https://app.notion.com/p/3ce69255c83981eb8c0cf50c90bed733?pvs=204>
