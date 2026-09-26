#!/usr/bin/env bash
# TokenRealty demo seed helper — verifies dev data and prints next Hardhat demo steps.
# Prerequisites: Postgres + Auth (:8083) + Registry (:8081) running with default dev seeds.

set -euo pipefail

AUTH_URL="${AUTH_URL:-http://localhost:8083/api}"
REGISTRY_URL="${REGISTRY_URL:-http://localhost:8081/api}"

echo "==> Logging in as admin..."
TOKEN=$(curl -sf "${AUTH_URL}/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@tokenrealty.com","password":"admin123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

echo "==> Fetching demo buildings..."
curl -sf "${REGISTRY_URL}/v1/buildings" \
  -H "Authorization: Bearer ${TOKEN}" \
  | python3 -m json.tool

cat <<'EOF'

Demo seed (on service startup):
  - Registry: Sunrise Tower, flat 101, SPV, valuation
  - Compliance: investor 11111111-1111-1111-1111-111111111111 (Hardhat account #1)
  - Auth: admin@tokenrealty.com / admin123, investor@tokenrealty.com / investor123

Next — full on-chain demo:
  1. docker compose up -d postgres
  2. docker compose --profile kafka up -d   # optional, for event-driven settlement
  3. cd token-issuance-service/hardhat && npm run node && npm run deploy:local
  4. Start services with --spring.profiles.active=local
  5. See docs/hardhat-demo.md for tokenize → list → buy flow

Disable dev seeds in production:
  SEED_DEV_PROPERTIES=false SEED_DEV_COMPLIANCE=false tokenrealty.auth.seed-dev-users=false
EOF
