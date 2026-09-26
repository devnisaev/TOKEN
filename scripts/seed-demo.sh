#!/usr/bin/env bash
# TokenRealty demo seed helper — verifies dev data and prints next Hardhat demo steps.
# Prerequisites: Postgres + Auth (:8083) + Registry (:8081) + Payment (:8085) running with dev seeds.

set -euo pipefail

AUTH_URL="${AUTH_URL:-http://localhost:8083/api}"
REGISTRY_URL="${REGISTRY_URL:-http://localhost:8081/api}"
PAYMENT_URL="${PAYMENT_URL:-http://localhost:8085/api}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080/api}"

DEMO_INVESTOR_ID="11111111-1111-1111-1111-111111111111"

echo "==> Logging in as admin..."
TOKEN=$(curl -sf "${AUTH_URL}/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@tokenrealty.com","password":"admin123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

echo "==> Fetching demo buildings..."
BUILDINGS_JSON=$(curl -sf "${REGISTRY_URL}/v1/buildings" -H "Authorization: Bearer ${TOKEN}")
echo "${BUILDINGS_JSON}" | python3 -m json.tool

echo "==> Checking demo investor USDC balance (Payment Service)..."
curl -sf "${PAYMENT_URL}/v1/wallet-balances/${DEMO_INVESTOR_ID}" \
  -H "Authorization: Bearer ${TOKEN}" \
  | python3 -m json.tool

echo "==> BFF flat detail (via API Gateway)..."
FLAT_ID=$(echo "${BUILDINGS_JSON}" | python3 -c "
import sys, json, urllib.request
data = json.load(sys.stdin)
content = data.get('content') or []
if not content:
    sys.exit(0)
building_id = content[0]['id']
token = sys.argv[1]
registry = sys.argv[2]
req = urllib.request.Request(
    f'{registry}/v1/buildings/{building_id}/flats',
    headers={'Authorization': f'Bearer {token}'})
with urllib.request.urlopen(req) as resp:
    flats = json.load(resp)
items = flats.get('content') or []
if items:
    print(items[0]['id'])
" "${TOKEN}" "${REGISTRY_URL}" 2>/dev/null || true)

if [ -n "${FLAT_ID:-}" ]; then
  curl -sf "${GATEWAY_URL}/v1/bff/flats/${FLAT_ID}" \
    -H "Authorization: Bearer ${TOKEN}" \
    | python3 -m json.tool || echo "(BFF skipped — start api-gateway :8080)"
else
  echo "(Skip BFF — no demo flat yet)"
fi

cat <<'EOF'

Demo seed (on service startup):
  - Registry: Sunrise Tower, flat 101, SPV, valuation
  - Compliance: investor 11111111-1111-1111-1111-111111111111 (Hardhat account #1)
  - Payment: demo investor seeded with 10,000 USDC (custodial balance)
  - Auth: admin@tokenrealty.com / admin123, investor@tokenrealty.com / investor123

Next — full on-chain demo:
  1. docker compose up -d postgres
  2. docker compose --profile kafka up -d   # optional, for event-driven settlement
  3. cd token-issuance-service/hardhat && npm run node && npm run deploy:local
  4. Start services with --spring.profiles.active=local
  5. See docs/hardhat-demo.md for tokenize → list → buy flow
  6. BFF: GET /api/v1/bff/flats/{flatId} via gateway :8080

Disable dev seeds in production:
  SEED_DEV_PROPERTIES=false SEED_DEV_COMPLIANCE=false tokenrealty.auth.seed-dev-users=false
  tokenrealty.payment.seed-dev-balances=false
EOF
