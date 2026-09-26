#!/usr/bin/env bash
# Place a demo primary-market buy order via API Gateway (requires demo stack + tokenized flat).
#
# Usage:
#   ./scripts/demo-buy-flow.sh [tokenAmount]
#
# Default tokenAmount is 1. Requires investor KYC, active listing, and Payment Service up.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080/api}"
AUTH_URL="${AUTH_URL:-http://localhost:8083/api}"
MARKETPLACE_URL="${MARKETPLACE_URL:-http://localhost:8084/api}"
TOKEN_AMOUNT="${1:-1}"

DEMO_FLAT_ID="33333333-3333-3333-3333-333333333333"

if ! curl -sf "${GATEWAY_URL%/api}/actuator/health" >/dev/null 2>&1; then
  echo "Gateway not reachable on :8080 — start demo stack first (./scripts/demo-all.sh)" >&2
  exit 1
fi

echo "==> Logging in as demo investor..."
INVESTOR_TOKEN=$(curl -sf "${AUTH_URL}/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"investor@tokenrealty.com","password":"investor123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

echo "==> Finding active listing for demo flat ${DEMO_FLAT_ID}..."
LISTING_ID=$(curl -sf "${MARKETPLACE_URL}/v1/listings?flatId=${DEMO_FLAT_ID}&size=1" \
  -H "Authorization: Bearer ${INVESTOR_TOKEN}" \
  | python3 -c "
import sys, json
data = json.load(sys.stdin)
items = data.get('content') or []
if not items:
    sys.exit(1)
print(items[0]['id'])
" 2>/dev/null || true)

if [[ -z "${LISTING_ID:-}" ]]; then
  echo "No active listing for demo flat — run ./scripts/seed-tokenize-demo.sh first" >&2
  exit 1
fi

echo "==> Placing buy order for ${TOKEN_AMOUNT} token(s) on listing ${LISTING_ID}..."
ORDER_JSON=$(curl -sf "${GATEWAY_URL}/v1/orders/buy" \
  -H "Authorization: Bearer ${INVESTOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "{
    \"listingId\": \"${LISTING_ID}\",
    \"tokenAmount\": ${TOKEN_AMOUNT}
  }")

echo "${ORDER_JSON}" | python3 -m json.tool
ORDER_ID=$(echo "${ORDER_JSON}" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

echo ""
echo "Buy order placed: ${ORDER_ID}"
echo "Track status: curl -s ${GATEWAY_URL}/v1/orders/${ORDER_ID} -H 'Authorization: Bearer ${INVESTOR_TOKEN}' | python3 -m json.tool"
