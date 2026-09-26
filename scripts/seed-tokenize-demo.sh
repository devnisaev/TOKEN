#!/usr/bin/env bash
# Tokenize demo flat and ensure a primary listing exists (when Hardhat + Issuance are up).
#
# Usage:
#   ./scripts/seed-tokenize-demo.sh
#
# Skips gracefully when issuance is down or flat is already tokenized.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

AUTH_URL="${AUTH_URL:-http://localhost:8083/api}"
ISSUANCE_URL="${ISSUANCE_URL:-http://localhost:8082/api}"
MARKETPLACE_URL="${MARKETPLACE_URL:-http://localhost:8084/api}"
REGISTRY_URL="${REGISTRY_URL:-http://localhost:8081/api}"

DEMO_FLAT_ID="33333333-3333-3333-3333-333333333333"
DEMO_BUILDING_ID="55555555-5555-5555-5555-555555555555"
DEMO_SPV_WALLET="0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266"

if ! curl -sf "${ISSUANCE_URL}/actuator/health" >/dev/null 2>&1; then
  echo "==> Issuance not reachable on :8082 — skip tokenize seed"
  exit 0
fi

echo "==> Logging in as admin..."
TOKEN=$(curl -sf "${AUTH_URL}/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@tokenrealty.com","password":"admin123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

echo "==> Checking demo flat token contract..."
if curl -sf "${ISSUANCE_URL}/v1/tokens/by-flat/${DEMO_FLAT_ID}" \
  -H "Authorization: Bearer ${TOKEN}" >/dev/null 2>&1; then
  echo "Demo flat already tokenized."
else
  if ! curl -sf http://localhost:8545 >/dev/null 2>&1; then
    echo "Hardhat node not detected on :8545 — relying on DevTokenContractInitializer or manual tokenize."
    exit 0
  fi

  echo "==> Issuing tokens for demo flat (requires Hardhat deploy)..."
  HTTP_CODE=$(curl -s -o /tmp/tokenize-response.json -w "%{http_code}" \
    -X POST "${ISSUANCE_URL}/v1/tokens" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "{
      \"flatId\": \"${DEMO_FLAT_ID}\",
      \"buildingId\": \"${DEMO_BUILDING_ID}\",
      \"tokenName\": \"Sunrise Flat 101 Token\",
      \"tokenSymbol\": \"SFT-101\",
      \"totalSupply\": 1000,
      \"tokenPriceUsd\": 45.00,
      \"spvWalletAddress\": \"${DEMO_SPV_WALLET}\"
    }")

  if [[ "${HTTP_CODE}" == "201" ]]; then
    echo "Token contract issued."
    python3 -m json.tool /tmp/tokenize-response.json
  else
    echo "Token issue returned HTTP ${HTTP_CODE} — see /tmp/tokenize-response.json"
    cat /tmp/tokenize-response.json || true
  fi
fi

echo "==> Waiting for primary listing on demo flat..."
for _ in $(seq 1 30); do
  LISTINGS=$(curl -sf "${MARKETPLACE_URL}/v1/listings?flatId=${DEMO_FLAT_ID}&size=1" \
    -H "Authorization: Bearer ${TOKEN}" || true)
  if echo "${LISTINGS}" | python3 -c "import sys,json; d=json.load(sys.stdin); exit(0 if (d.get('content') or []) else 1)" 2>/dev/null; then
    echo "Primary listing found for demo flat."
    echo "${LISTINGS}" | python3 -m json.tool
    exit 0
  fi
  sleep 2
done

echo "No listing yet — ensure marketplace Kafka consumer is running (flat.tokenized)."
curl -sf "${REGISTRY_URL}/v1/flats/${DEMO_FLAT_ID}" \
  -H "Authorization: Bearer ${TOKEN}" \
  | python3 -m json.tool || true
