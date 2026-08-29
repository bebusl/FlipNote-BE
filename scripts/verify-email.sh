#!/usr/bin/env bash
# 로컬 전용 이메일 인증 우회 헬퍼.
#
# FlipNote는 SMTP가 아니라 Resend HTTP API로 메일을 보내기 때문에 MailHog로 가로챌 수 없다.
# 대신 인증 코드가 Redis에 저장되므로 거기서 직접 읽어 인증까지 끝낸다.
#
#   사용법: ./scripts/verify-email.sh <email>
set -euo pipefail

EMAIL="${1:-}"
if [[ -z "$EMAIL" ]]; then
  echo "usage: $0 <email>" >&2
  exit 1
fi

GATEWAY="${GATEWAY:-http://localhost:8080}"
cd "$(dirname "$0")/.."

# 이미 발급된 코드가 남아 있으면 재요청이 409로 막히므로 지우고 시작한다.
docker compose exec -T redis redis-cli DEL "email:verification:code:$EMAIL" >/dev/null

curl -sf -o /dev/null -X POST "$GATEWAY/v1/auth/email-verification/request" \
  -H 'Content-Type: application/json' -d "{\"email\":\"$EMAIL\"}"

CODE=$(docker compose exec -T redis redis-cli GET "email:verification:code:$EMAIL" | tr -d '\r')
if [[ -z "$CODE" ]]; then
  echo "인증 코드를 Redis에서 찾지 못했습니다. user-service 로그를 확인하세요." >&2
  exit 1
fi

curl -sf -o /dev/null -X POST "$GATEWAY/v1/auth/email-verification" \
  -H 'Content-Type: application/json' -d "{\"email\":\"$EMAIL\",\"code\":\"$CODE\"}"

echo "인증 완료: $EMAIL (code=$CODE, 10분 내에 회원가입 요청을 보내세요)"
