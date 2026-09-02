# Next-session deployment context

> **대체됨:** 현재 작업 컨텍스트는 [`docs/agent-context.md`](../agent-context.md)를 따른다.
> 이 문서는 과거 배포 메모이며, 현재 로컬 Compose·루트 워크플로·CI 설정과 다를 수 있다.

Last updated: 2026-08-29

## Repository state

- Public BE monorepo: https://github.com/bebusl/FlipNote-BE
- Public Infra GitOps repository: https://github.com/bebusl/FlipNote-Infra
- All seven container images target the public `ghcr.io/bebusl/flipnote-*` namespace.
- The root repository contains the original service and Infra histories through Git subtree merges.

## Local environment

- Start with `docker compose up -d`.
- Current infrastructure is MySQL, Redis, and RabbitMQ; the API Gateway is exposed at `http://localhost:8080`.
- `.env` is ignored. Copy `.env.example` for a clean setup.
- `JWT_SECRET` must be shared by user-service and cardset-service.
- The notification service needs valid Firebase service-account JSON; the local guide contains a dummy-key option.
- Service runtime was verified: all application and infrastructure containers were running; Gateway health returned HTTP 200.

## Deployment configuration already prepared

- Root `.github/workflows/cd-*.yml` builds each service only when its directory changes.
- The workflows publish to GHCR using `GITHUB_TOKEN` with `packages: write`.
- Helm charts support direct Kubernetes Secret injection through `existingSecret`; Vault is disabled by default.
- Datastore Helm value templates are under `FlipNote-Infra/deploy/`.
- App charts use `ghcr.io/bebusl/flipnote-*`, and ArgoCD source overrides are reset to the `main` tag for the first image publish.

## Before actual deployment

Decide and provide:

1. API domain, for example `api.flipnote.site`.
2. Frontend domain, for example `app.flipnote.site`.
3. VM access: public IP/hostname, SSH user, and authentication method.
4. DNS access or confirmation that the API domain A/AAAA record points to the VM.

At deployment time, create the following outside Git and do not send secret values in chat:

- Google OAuth client ID/secret and redirect URI
- Resend API key and verified sender domain
- Firebase service-account JSON
- S3 bucket and IAM credentials
- JWT, MySQL, Redis, and RabbitMQ credentials
- ArgoCD Git write credential if image-updater write-back remains enabled

## Known follow-ups

- Gateway health is healthy. The existing proxied Group health route returns 500, and the existing Cardset health route returns 404 because Cardset has no HTTP health endpoint. This does not prevent service startup, but the routes should be corrected before production monitoring depends on them.
- The local images are being rebuilt after this document is written so the latest User, Group, and Cardset source changes are exercised.

## Useful commands

`docker compose up -d`

`docker compose ps`

`docker compose logs -f api-gateway`

`./scripts/verify-email.sh test@flipnote.test`
